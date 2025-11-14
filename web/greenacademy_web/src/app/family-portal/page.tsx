import { Suspense } from "react";
import FamilyPortalClient from "./FamilyPortalClient";

export default function FamilyPortalPageWrapper() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <FamilyPortalClient />
    </Suspense>
  );
}
