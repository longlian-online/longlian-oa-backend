#!/bin/sh

# ============================================================================
# op.sh - 项目操作脚本
# 支持可扩展的命令系统
# ============================================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        log_error "命令 '$1' 未找到，请安装后重试"
        return 1
    fi
    return 0
}

# 显示帮助信息
show_help() {
    cat << EOF
op.sh - 项目操作脚本

用法:
  ./op.sh <命令> [参数]

可用命令:
  otel-init    初始化 OpenTelemetry Java Agent
  help         显示此帮助信息

示例:
  ./op.sh otel-init
  ./op.sh help

EOF
}

# otel-init 命令
otel_init_command() {
    log_info "正在检查 OpenTelemetry Java Agent..."
    
    local agent_file="opentelemetry-javaagent.jar"
    local download_url="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar"
    
    # 检查文件是否已存在
    if [ -f "$agent_file" ]; then
        log_success "文件 '$agent_file' 已存在，无需下载"
        log_info "文件信息:"
        ls -lh "$agent_file"
        return 0
    fi
    
    log_info "文件 '$agent_file' 不存在，开始下载..."
    
    # 检查 curl 命令
    if ! check_command "curl"; then
        log_error "请安装 curl 后重试"
        return 1
    fi
    
    # 下载文件
    log_info "从 $download_url 下载..."
    
    if curl -L --progress-bar -o "$agent_file" "$download_url"; then
        if [ -f "$agent_file" ]; then
            file_size=$(stat -f%z "$agent_file" 2>/dev/null || stat -c%s "$agent_file" 2>/dev/null || echo "未知")
            log_success "下载完成！文件大小: $file_size 字节"
            log_info "文件已保存到: $(pwd)/$agent_file"
            return 0
        else
            log_error "下载失败，文件未创建"
            return 1
        fi
    else
        log_error "下载过程中出现错误"
        if [ -f "$agent_file" ]; then
            rm -f "$agent_file"
        fi
        return 1
    fi
}

# 主函数
main() {
    # 如果没有参数，显示帮助信息
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    local command="$1"
    shift
    
    case "$command" in
        otel-init)
            otel_init_command "$@"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $command"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
