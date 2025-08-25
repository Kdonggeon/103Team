import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Frame } from "./screens/Frame/Frame";

createRoot(document.getElementById("app")).render(
  <StrictMode>
    <Frame />
  </StrictMode>,
);
