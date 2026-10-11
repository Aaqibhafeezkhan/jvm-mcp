class JvmMcp < Formula
  desc "Native Java Model Context Protocol (MCP) server for live Spring Boot inspection"
  homepage "https://github.com/oscarbol09/jvm-mcp"
  url "https://github.com/oscarbol09/jvm-mcp/releases/download/v1.0.0/jvm-mcp-macos-aarch64.tar.gz"
  version "1.0.0"
  sha256 "PLACEHOLDER"

  def install
    libexec.install Dir["*"]
    bin.install_symlink libexec/"jvm-mcp"
  end

  test do
    system "#{bin}/jvm-mcp", "--version"
  end
end
