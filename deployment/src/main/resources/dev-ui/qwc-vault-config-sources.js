import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';

const DEFAULT_ENGINE = '<default>';
const LOAD_TIMEOUT_MILLIS = 10000;
const DOCS_URL = 'https://docs.quarkiverse.io/quarkus-vault/dev/index.html';

export class QwcVaultConfigSources extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: block;
            padding: var(--lumo-space-m);
            color: var(--lumo-body-text-color);
            font-family: var(--lumo-font-family);
        }
        .path {
            margin-bottom: var(--lumo-space-m);
        }
        .meta {
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-secondary-text-color);
        }
        code {
            display: block;
            font-family: var(--lumo-font-family-mono, monospace);
            font-size: var(--lumo-font-size-s);
        }
        .error {
            color: var(--lumo-error-text-color);
            font-family: var(--lumo-font-family-mono, monospace);
            font-size: var(--lumo-font-size-s);
            word-break: break-word;
        }
    `;

    static properties = {
        _snapshot: { state: true },
        _loading: { state: true },
        _error: { state: true }
    };

    constructor() {
        super();
        this._loading = true;
        this._snapshot = null;
        this._error = null;
        this._requestId = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._requestId++;
    }

    async hotReload() {
        const requestId = ++this._requestId;
        this._loading = true;
        this._snapshot = null;
        this._error = null;
        let timeout;
        try {
            const result = await Promise.race([
                this.jsonRpc.getConfigSources(),
                new Promise((resolve, reject) => {
                    timeout = setTimeout(() => reject(new Error('Request timed out')), LOAD_TIMEOUT_MILLIS);
                })
            ]);
            if (requestId === this._requestId) {
                this._snapshot = result.result;
            }
        } catch {
            if (requestId === this._requestId) {
                this._error = 'Unable to load config source metadata. Check that the application is running and try again.';
            }
        } finally {
            clearTimeout(timeout);
            if (requestId === this._requestId) {
                this._loading = false;
            }
        }
    }

    render() {
        if (this._loading) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        if (this._error) {
            return html`<p class="error" role="alert">${this._error}</p>
                <vaadin-button @click=${this.hotReload}>Retry</vaadin-button>`;
        }
        if (!this._snapshot || this._snapshot.paths.length === 0) {
            return html`<p>No KV path is read by the Vault config source. Set <code>quarkus.vault.secret-config-kv-path</code> to load secrets as configuration properties. See the <a href="${DOCS_URL}" target="_blank">Vault extension documentation</a>.</p>`;
        }
        return html`
            <p class="meta">Fetched at ${this._snapshot.fetchedAt}</p>
            ${this._snapshot.paths.map(path => this._renderPath(path))}
        `;
    }

    _renderPath(path) {
        const details = [];
        if (path.engine !== DEFAULT_ENGINE) {
            details.push(`engine: ${path.engine}`);
        }
        if (path.prefix) {
            details.push(`prefix: ${path.prefix}`);
        }
        return html`
            <div class="path">
                <h4>${path.path}</h4>
                ${details.length > 0 ? html`<div class="meta">${details.join(' | ')}</div>` : ''}
                ${path.error
                    ? html`<div class="error">${path.error}</div>`
                    : path.propertyNames.length === 0
                        ? html`<div class="meta">No properties</div>`
                        : path.propertyNames.map(name => html`<code>${name}</code>`)}
            </div>
        `;
    }
}
customElements.define('qwc-vault-config-sources', QwcVaultConfigSources);
