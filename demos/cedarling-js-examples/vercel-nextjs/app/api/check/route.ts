import type { NextRequest } from "next/server";

import { handlePermissionCheck } from "@/libs/permission-check";

// With no runtime override, Next.js exercises the SDK's Node entry point.
export async function GET(request: NextRequest) {
  return handlePermissionCheck(request);
}
