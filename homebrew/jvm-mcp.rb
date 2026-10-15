class JvmMcp < Formula
  desc "Native Java Model Context Protocol (MCP) server for live Spring Boot inspection"
  homepage "https://github.com/oscarbol09/jvm-mcp"
  version "1.1.0"

  on_macos do
    on_arm do
      url "https://github.com/oscarbol09/jvm-mcp/releases/download/v#{version}/jvm-mcp-macos-aarch64.tar.gz"
      sha256 "REPLACE_WITH_MACOS_AARCH64_SHA256"
    end
    on_intel do
      url "https://github.com/oscarbol09/jvm-mcp/releases/download/v#{version}/jvm-mcp-macos-amd64.tar.gz"
      sha256 "REPLACE_WITH_MACOS_AMD64_SHA256"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/oscarbol09/jvm-mcp/releases/download/v#{version}/jvm-mcp-linux-amd64.tar.gz"
      sha256 "REPLACE_WITH_LINUX_AMD64_SHA256"
    end
    on_arm do
      url "https://github.com/oscarbol09/jvm-mcp/releases/download/v#{version}/jvm-mcp-linux-aarch64.tar.gz"
      sha256 "REPLACE_WITH_LINUX_AARCH64_SHA256"
    end
  end

  def install
    libexec.install Dir["*"]
    bin.install_symlink libexec/"jvm-mcp"
  end

  test do
    system "#{bin}/jvm-mcp", "--version"
  end
end
