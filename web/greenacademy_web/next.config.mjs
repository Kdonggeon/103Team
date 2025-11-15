// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/backend/:path*',
        destination: 'http://13.217.211.242:9090/:path*', // EC2로 프록시
      },
    ];
  },
};

export default nextConfig;
