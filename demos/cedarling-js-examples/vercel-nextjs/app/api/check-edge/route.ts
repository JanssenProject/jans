import type { NextRequest } from "next/server";

import { handlePermissionCheck } from "@/libs/permission-check";

// This thin adapter makes the same SDK flow compile and run in the Edge runtime.
export const runtime = "edge";

export async function GET(request: NextRequest) {
  return handlePermissionCheck(request, runtime);
}
