#!/bin/bash

# Docker Compose 启动脚本
# 支持灵活的环境变量配置

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查必需的环境变量
check_required_env() {
    if [ -z "$DEEPSEEK_API_KEY" ]; then
        print_error "DEEPSEEK_API_KEY 环境变量未设置！"
        echo "请使用以下方式之一设置："
        echo "  1. export DEEPSEEK_API_KEY=your_api_key"
        echo "  2. DEEPSEEK_API_KEY=your_api_key ./start-docker.sh"
        echo "  3. 在 .env 文件中配置"
        exit 1
    fi
}

# 检查Docker和Docker Compose
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装！请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose 未安装！请先安装 Docker Compose"
        exit 1
    fi
    
    print_message "Docker 环境检查通过"
}

# 加载环境变量
load_env() {
    if [ -f .env ]; then
        print_message "加载 .env 文件"
        export $(cat .env | grep -v '^#' | xargs)
    fi
}

# 构建镜像
build_image() {
    print_message "开始构建 Docker 镜像..."
    docker-compose build --no-cache
    if [ $? -eq 0 ]; then
        print_message "镜像构建成功"
    else
        print_error "镜像构建失败"
        exit 1
    fi
}

# 启动服务
start_services() {
    print_message "启动服务..."
    
    # 使用提供的数据库配置或默认值
    export MYSQL_HOST=${MYSQL_HOST:-localhost}
    export MYSQL_PORT=${MYSQL_PORT:-3306}
    export MYSQL_DATABASE=${MYSQL_DATABASE:-test}
    export MYSQL_USERNAME=${MYSQL_USERNAME:-root}
    export MYSQL_PASSWORD=${MYSQL_PASSWORD:-root}
    
    print_message "数据库配置："
    echo "  Host: $MYSQL_HOST:$MYSQL_PORT"
    echo "  Database: $MYSQL_DATABASE"
    echo "  Username: $MYSQL_USERNAME"
    
    # 启动服务
    docker-compose up -d
    
    if [ $? -eq 0 ]; then
        print_message "服务启动成功"
        print_message "应用地址: http://localhost:8090"
        print_message "查看日志: docker-compose logs -f springai-app"
    else
        print_error "服务启动失败"
        exit 1
    fi
}

# 停止服务
stop_services() {
    print_message "停止服务..."
    docker-compose down
    print_message "服务已停止"
}

# 查看日志
view_logs() {
    docker-compose logs -f springai-app
}

# 清理所有（包括数据卷）
clean_all() {
    print_warning "即将清理所有容器、镜像和数据卷"
    read -p "确定要继续吗？(y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v --rmi all
        print_message "清理完成"
    else
        print_message "操作已取消"
    fi
}

# 主菜单
show_menu() {
    echo "======================================"
    echo "   SpringAI-MCP Docker 管理脚本"
    echo "======================================"
    echo "1. 构建并启动服务"
    echo "2. 仅构建镜像"
    echo "3. 启动服务"
    echo "4. 停止服务"
    echo "5. 查看日志"
    echo "6. 重启服务"
    echo "7. 清理所有"
    echo "0. 退出"
    echo "======================================"
}

# 主函数
main() {
    # 检查Docker环境
    check_docker
    
    # 加载环境变量
    load_env
    
    # 如果没有参数，显示菜单
    if [ $# -eq 0 ]; then
        while true; do
            show_menu
            read -p "请选择操作 [0-7]: " choice
            case $choice in
                1)
                    check_required_env
                    build_image
                    start_services
                    ;;
                2)
                    build_image
                    ;;
                3)
                    check_required_env
                    start_services
                    ;;
                4)
                    stop_services
                    ;;
                5)
                    view_logs
                    ;;
                6)
                    check_required_env
                    stop_services
                    start_services
                    ;;
                7)
                    clean_all
                    ;;
                0)
                    print_message "退出"
                    exit 0
                    ;;
                *)
                    print_error "无效的选择"
                    ;;
            esac
            echo
            read -p "按Enter键继续..."
        done
    else
        # 命令行模式
        case $1 in
            start)
                check_required_env
                build_image
                start_services
                ;;
            stop)
                stop_services
                ;;
            restart)
                check_required_env
                stop_services
                start_services
                ;;
            build)
                build_image
                ;;
            logs)
                view_logs
                ;;
            clean)
                clean_all
                ;;
            *)
                echo "Usage: $0 {start|stop|restart|build|logs|clean}"
                exit 1
                ;;
        esac
    fi
}

# 执行主函数
main "$@"