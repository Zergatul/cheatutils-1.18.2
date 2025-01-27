async function loadVue() {
    try {
        const vue = await import('https://cdn.jsdelivr.net/npm/vue@3.2.33/dist/vue.esm-browser.prod.js');
        console.debug('Vue has been loaded from CDN.');
        return vue;
    } catch (error) {
        console.warn('Failed to load Vue from CDN, falling back to local version.', error);
        try {
            const vue = await import('/local/vue.esm-browser.prod.js');
            console.debug('Vue has been loaded from local server.')
            return vue;
        } catch (error) {
            console.error('Failed to load Vue from local server.', error);
            alert('Vue could not be loaded.');
            throw new Error('Vue could not be loaded.');
        }
    }
}

async function loadMonaco() {
    try {
        const monaco = await import('https://cdn.jsdelivr.net/npm/monaco-editor@0.48.0/+esm');
        console.debug('Monaco has been loaded from CDN.');
        return monaco;
    } catch (error) {
        console.warn('Failed to load Monaco from CDN, falling back to local version.', error);
        try {
            const monaco = await import('/local/monaco-editor.js');
            console.debug('Monaco has been loaded from local server.')
            return monaco;
        } catch (error) {
            console.error('Failed to load Monaco from local server.', error);
            alert('Monaco could not be loaded.');
            throw new Error('Monaco could not be loaded.');
        }
    }
}

let vuePromise = null;
let monacoPromise = null;

export function vue() {
    if (!vuePromise) {
        vuePromise = loadVue();
    }
    return vuePromise;
}

export function monaco() {
    if (!monacoPromise) {
        monacoPromise = loadMonaco();
    }
    return monacoPromise;
}