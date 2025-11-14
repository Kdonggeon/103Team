// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/backend/:path*',
        // 🔥 EC2 백엔드 절대 주소로 변경
        destination: 'http://13.217.211.242:9090/:path*',
      },
    ];
  },
};

export default nextConfig;
