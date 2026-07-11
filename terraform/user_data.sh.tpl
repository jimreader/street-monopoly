#!/bin/bash
set -euo pipefail

# Log everything
exec > >(tee /var/log/user-data.log) 2>&1
echo "=== Road Rush EC2 setup starting ==="

# Install Java 21
dnf install -y java-21-amazon-corretto-headless
# Install CloudWatch agent for centralized logs
dnf install -y amazon-cloudwatch-agent

# Create app user and directory
mkdir -p /opt/streetmonopoly
useradd -r -s /bin/false streetmonopoly || true
chown -R streetmonopoly:streetmonopoly /opt/streetmonopoly
mkdir -p /var/log/streetmonopoly
touch /var/log/streetmonopoly/app.log
chown -R streetmonopoly:streetmonopoly /var/log/streetmonopoly

# Write environment file
cat > /opt/streetmonopoly/app.env << 'ENVEOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://${db_endpoint}/${db_name}
SPRING_DATASOURCE_USERNAME=${db_username}
SPRING_DATASOURCE_PASSWORD=${db_password}
AUTH0_DOMAIN=${auth0_domain}
AUTH0_AUDIENCE=${auth0_audience}
MAIL_USERNAME=${mail_username}
MAIL_PASSWORD=${mail_password}
APP_ADMIN_URL=${admin_url}
APP_PLAYER_URL=${player_url}
APP_API_URL=http://localhost:8080
APP_CORS_ALLOWED_ORIGINS=${admin_url},${player_url}
IMAGES_BUCKET=${images_bucket}
AWS_REGION=${aws_region}
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC
ENVEOF

chmod 600 /opt/streetmonopoly/app.env
chown streetmonopoly:streetmonopoly /opt/streetmonopoly/app.env

# Create systemd service
cat > /etc/systemd/system/streetmonopoly.service << 'SVCEOF'
[Unit]
Description=Road Rush API
After=network.target

[Service]
Type=simple
User=streetmonopoly
WorkingDirectory=/opt/streetmonopoly
EnvironmentFile=/opt/streetmonopoly/app.env
ExecStart=/usr/bin/java -jar /opt/streetmonopoly/app.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/streetmonopoly/app.log
StandardError=append:/var/log/streetmonopoly/app.log

[Install]
WantedBy=multi-user.target
SVCEOF

systemctl daemon-reload
systemctl enable streetmonopoly

# Configure CloudWatch log shipping
mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'CWEOF'
{
	"logs": {
		"logs_collected": {
			"files": {
				"collect_list": [
					{
						"file_path": "/var/log/streetmonopoly/app.log",
						"log_group_name": "/${app_name}/api",
						"log_stream_name": "{instance_id}",
						"retention_in_days": 14
					},
					{
						"file_path": "/var/log/user-data.log",
						"log_group_name": "/${app_name}/bootstrap",
						"log_stream_name": "{instance_id}",
						"retention_in_days": 7
					}
				]
			}
		}
	}
}
CWEOF

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s

echo "=== Setup complete. Upload app.jar to /opt/streetmonopoly/ and run: sudo systemctl start streetmonopoly ==="
