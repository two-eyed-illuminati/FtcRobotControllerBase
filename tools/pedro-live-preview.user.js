// ==UserScript==
// @name         Pedro Pathing live preview
// @namespace    https://github.com/Pedro-Pathing/Visualizer
// @version      0.3.0
// @description  Reload a generated .pp file when it changes on disk.
// @match        https://visualizer.pedropathing.com/*
// @run-at       document-start
// @grant        none
// ==/UserScript==

(() => {
    "use strict";
    console.log("Pedro live preview 0.3.0 loaded");

    const POLL_INTERVAL_MS = 500;
    let fileHandle;
    let lastModified;
    let reloading = false;
    let openInput;

    // The Visualizer (2.0.0+) builds a throwaway <input type="file"> each time
    // "Open from computer…" runs. Keep its handler and answer it with the watched
    // file instead of showing the native dialog.
    const nativeClick = HTMLInputElement.prototype.click;
    HTMLInputElement.prototype.click = function (...args) {
        if (this.type !== "file" || !this.onchange || !fileHandle) {
            return nativeClick.apply(this, args);
        }
        console.log("Pedro live preview: captured the Visualizer's file input");
        openInput = this;
        reloadIfChanged(true);
    };
    console.log("Pedro live preview: file input hook installed");

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
            button.textContent = "Attaching…";
            await openThroughFileMenu();
        } catch (error) {
            if (error.name !== "AbortError") showError(error);
        }
    });

    function findByText(selector, prefix) {
        return [...document.querySelectorAll(selector)]
            .find((element) => element.textContent.trim().startsWith(prefix));
    }

    async function waitFor(find, timeoutMs = 2000) {
        const deadline = Date.now() + timeoutMs;
        for (let element = find(); !element; element = find()) {
            if (Date.now() > deadline) return null;
            await new Promise((resolve) => setTimeout(resolve, 50));
        }
        return find();
    }

    async function openThroughFileMenu() {
        const fileMenu = findByText('button[aria-haspopup="menu"]', "File");
        if (!fileMenu) return showHint("Could not find the File menu");

        fileMenu.click();
        const item = await waitFor(() => findByText('[role="menuitem"]', "Open from computer"));
        if (!item) return showHint("Could not find File > Open from computer…");

        item.click();
    }

    function showHint(reason) {
        console.log(`Pedro live preview: ${reason}`);
        button.textContent = "Now: File > Open from computer…";
        button.title = `${reason}. Run the Visualizer's "Open from computer…" action once to attach the watched file.`;
    }

    async function reloadIfChanged(force = false) {
        if (!fileHandle || !openInput || reloading) return;

        reloading = true;
        try {
            const file = await fileHandle.getFile();
            if (!force && file.lastModified === lastModified) return;

            const transfer = new DataTransfer();
            transfer.items.add(file);
            console.log(`Pedro live preview: loading ${file.name} (${file.size} bytes)`);
            openInput.files = transfer.files;
            openInput.dispatchEvent(new Event("change", {bubbles: true}));

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

    function attachButton() {
        if (document.body && !button.isConnected) document.body.appendChild(button);
    }
    attachButton();
    document.addEventListener("DOMContentLoaded", attachButton);
    new MutationObserver(attachButton).observe(document.documentElement, {childList: true, subtree: true});
    window.setInterval(reloadIfChanged, POLL_INTERVAL_MS);
})();
