#!/bin/bash
set -euo pipefail

# Log everything
exec > >(tee /var/log/user-data.log) 2>&1
echo "=== Road Rush EC2 setup starting ==="

# Install Java 21
dnf install -y java-21-amazon-corretto-headless

# Create app user
useradd -r -s /bin/false streetmonopoly || true
mkdir -p /opt/streetmonopoly/uploads
chown -R streetmonopoly:streetmonopoly /opt/streetmonopoly

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
APP_UPLOAD_DIR=/opt/streetmonopoly/uploads
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
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SVCEOF

systemctl daemon-reload
systemctl enable streetmonopoly

echo "=== Setup complete. Upload app.jar to /opt/streetmonopoly/ and run: sudo systemctl start streetmonopoly ==="
