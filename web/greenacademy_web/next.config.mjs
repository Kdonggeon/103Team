// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/backend/:path*',
        destination: 'http://localhost:9090/:path*', // 백엔드 주소/포트
      },
    ];
  },
};

export default nextConfig;
