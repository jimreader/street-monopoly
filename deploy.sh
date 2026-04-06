#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Road Rush — Build & Deploy (EC2 edition)
#
# Prerequisites:
#   - AWS CLI configured
#   - Java 21+ and Maven (for backend build)
#   - Node.js 18+ and npm (for frontend builds)
#   - Terraform applied (cd terraform && terraform apply)
#   - SSH key configured if using ec2_key_name, OR use SSM Session Manager
#
# Usage:
#   ./deploy.sh                 # deploy everything
#   ./deploy.sh backend         # deploy only the backend
#   ./deploy.sh admin           # deploy only the admin app
#   ./deploy.sh player          # deploy only the player app
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REGION=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw aws_region 2>/dev/null || echo "eu-west-2")

echo "📦 Reading Terraform outputs..."
EC2_IP=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw ec2_public_ip)
EC2_ID=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw ec2_instance_id)
ADMIN_BUCKET=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw admin_s3_bucket)
PLAYER_BUCKET=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw player_s3_bucket)
ADMIN_CF_ID=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw admin_cloudfront_id)
PLAYER_CF_ID=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw player_cloudfront_id)
ADMIN_URL=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw admin_app_url)
PLAYER_URL=$(cd "$SCRIPT_DIR/terraform" && terraform output -raw player_app_url)

COMPONENT="${1:-all}"

# ---------------------------------------------------------------------------
deploy_backend() {
  echo ""
  echo "🔨 Building backend..."
  cd "$SCRIPT_DIR/backend"
  mvn clean package -DskipTests

  echo "📤 Uploading JAR via S3..."
  aws s3 cp target/street-monopoly-api-1.0.0.jar "s3://${ADMIN_BUCKET}/deploy/app.jar" --region "$REGION"

  echo "🔄 Deploying to EC2 via SSM..."
  COMMAND_ID=$(aws ssm send-command \
    --instance-ids "$EC2_ID" \
    --document-name "AWS-RunShellScript" \
    --parameters "commands=[
      'sed -i \"s|^APP_ADMIN_URL=.*|APP_ADMIN_URL=${ADMIN_URL}|\" /opt/streetmonopoly/app.env',
      'sed -i \"s|^APP_PLAYER_URL=.*|APP_PLAYER_URL=${PLAYER_URL}|\" /opt/streetmonopoly/app.env',
      'sed -i \"s|^APP_CORS_ALLOWED_ORIGINS=.*|APP_CORS_ALLOWED_ORIGINS=${ADMIN_URL},${PLAYER_URL}|\" /opt/streetmonopoly/app.env',
      'aws s3 cp s3://${ADMIN_BUCKET}/deploy/app.jar /opt/streetmonopoly/app.jar --region ${REGION}',
      'chown streetmonopoly:streetmonopoly /opt/streetmonopoly/app.jar',
      'systemctl restart streetmonopoly'
    ]" \
    --region "$REGION" \
    --query "Command.CommandId" --output text)

  echo "   Waiting for deployment to complete (command: $COMMAND_ID)..."
  aws ssm wait command-executed \
    --command-id "$COMMAND_ID" \
    --instance-id "$EC2_ID" \
    --region "$REGION" 2>/dev/null || true

  aws s3 rm "s3://${ADMIN_BUCKET}/deploy/app.jar" --region "$REGION" 2>/dev/null || true

  echo "✅ Backend deployed. It may take 15-30 seconds to start."
}

# ---------------------------------------------------------------------------
deploy_admin() {
  echo ""
  echo "🔨 Building admin app..."
  cd "$SCRIPT_DIR/admin-app"
  npm install
  npm run build

  echo "📤 Uploading to S3..."
  aws s3 sync dist/ "s3://$ADMIN_BUCKET" --delete --region "$REGION"

  echo "🔄 Invalidating CloudFront cache..."
  aws cloudfront create-invalidation --distribution-id "$ADMIN_CF_ID" --paths "/*" > /dev/null

  echo "✅ Admin app deployed at: $ADMIN_URL"
}

# ---------------------------------------------------------------------------
deploy_player() {
  echo ""
  echo "🔨 Building player app..."
  cd "$SCRIPT_DIR/player-app"
  npm install
  npm run build

  echo "📤 Uploading to S3..."
  aws s3 sync dist/ "s3://$PLAYER_BUCKET" --delete --region "$REGION"

  echo "🔄 Invalidating CloudFront cache..."
  aws cloudfront create-invalidation --distribution-id "$PLAYER_CF_ID" --paths "/*" > /dev/null

  echo "✅ Player app deployed at: $PLAYER_URL"
}

# ---------------------------------------------------------------------------
case "$COMPONENT" in
  backend) deploy_backend ;;
  admin)   deploy_admin ;;
  player)  deploy_player ;;
  all)
    deploy_backend
    deploy_admin
    deploy_player
    echo ""
    echo "============================================"
    echo "🎉 ALL COMPONENTS DEPLOYED"
    echo "============================================"
    echo "  Admin:  $ADMIN_URL"
    echo "  Player: $PLAYER_URL"
    echo "  API:    http://${EC2_IP}:8080"
    echo ""
    echo "  Update Auth0 callback URLs with the"
    echo "  admin CloudFront domain above."
    echo "============================================"
    ;;
  *) echo "Usage: $0 [backend|admin|player|all]"; exit 1 ;;
esac
