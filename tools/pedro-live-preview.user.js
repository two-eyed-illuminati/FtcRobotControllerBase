// ==UserScript==
// @name         Pedro Pathing live preview
// @namespace    https://github.com/Pedro-Pathing/Visualizer
// @version      0.1.0
// @description  Reload a generated .pp file when it changes on disk.
// @match        https://visualizer.pedropathing.com/*
// @grant        none
// ==/UserScript==

(() => {
    "use strict";

    const POLL_INTERVAL_MS = 500;
    let fileHandle;
    let lastModified;
    let reloading = false;

    const button = document.createElement("button");
    button.textContent = "Watch .pp";
    button.title = "Select a generated .pp file to reload when it changes";
    Object.assign(button.style, {
        position: "fixed",
        right: "16px",
        bottom: "16px",
        zIndex: "2147483647",
        padding: "10px 14px",
        border: "1px solid #555",
        borderRadius: "8px",
        background: "#1a1a1a",
        color: "#d8d8d8",
        font: "600 14px system-ui, sans-serif",
        cursor: "pointer",
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.35)",
    });

    button.addEventListener("click", async () => {
        if (typeof window.showOpenFilePicker !== "function") {
            alert(
                "This browser does not support showOpenFilePicker(). " +
                "Use Chrome or Edge for live .pp file watching."
            );
            return;
        }

        try {
            [fileHandle] = await window.showOpenFilePicker({
                id: "pedro-live-preview",
                multiple: false,
                types: [{
                    description: "Pedro Pathing project",
                    accept: {"application/json": [".pp", ".json"]},
                }],
            });
            lastModified = undefined;
            await reloadIfChanged();
        } catch (error) {
            if (error.name !== "AbortError") showError(error);
        }
    });

    async function reloadIfChanged() {
        if (!fileHandle || reloading) return;

        reloading = true;
        try {
            const file = await fileHandle.getFile();
            if (file.lastModified === lastModified) return;

            const input = document.querySelector("#file-input");
            if (!(input instanceof HTMLInputElement)) {
                throw new Error("Could not find the Visualizer's .pp file input");
            }

            const transfer = new DataTransfer();
            transfer.items.add(file);
            input.files = transfer.files;
            input.dispatchEvent(new Event("change", {bubbles: true}));

            lastModified = file.lastModified;
            button.textContent = `Watching ${file.name}`;
            button.style.borderColor = "#22c55e";
            button.style.color = "#86efac";
            button.title = `Last reloaded at ${new Date().toLocaleTimeString()}`;
        } catch (error) {
            showError(error);
        } finally {
            reloading = false;
        }
    }

    function showError(error) {
        console.error("Pedro live preview:", error);
        button.textContent = "Watch failed";
        button.style.borderColor = "#ef4444";
        button.style.color = "#fca5a5";
        button.title = String(error);
    }

    document.body.appendChild(button);
    window.setInterval(reloadIfChanged, POLL_INTERVAL_MS);
})();
