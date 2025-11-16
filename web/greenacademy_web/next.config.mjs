// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/backend/:path*',
        destination: 'http://13.217.211.242:9090/:path*', // www.greenacademy.co.kr backend server
      },
    ];
  },
};

export default nextConfig;
