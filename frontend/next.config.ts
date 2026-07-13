import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Allow dev-server (HMR) requests when the app is opened via the LAN IP
  allowedDevOrigins: ["10.14.0.2"],
  // Two package-lock.json files exist (repo root + frontend); pin the root so
  // Turbopack doesn't pick the repo root by mistake
  turbopack: {
    root: __dirname,
  },
};

export default nextConfig;
