'use strict';
import * as vscode from 'vscode';
import {
    CancellationToken,
    DebugConfiguration,
    ExtensionContext, Uri,
    ViewColumn,
    window,
    workspace,
    WorkspaceFolder
} from 'vscode';
import {
    ClientCapabilities,
    DocumentSelector,
    DynamicFeature,
    InitializeParams,
    LanguageClient,
    LanguageClientOptions,
    RegistrationData,
    RPCMessageType,
    ServerCapabilities,
    ServerOptions
} from 'vscode-languageclient';
import {XMLHttpRequest} from 'xmlhttprequest-ts';
import * as crypto from "crypto";

// Track currently webview panel
let panel: vscode.WebviewPanel | undefined = undefined;

export function activate(context: ExtensionContext) {
    const adbSocketPath = `gobpie_adb_${crypto.randomBytes(6).toString('base64url')}.sock`

    const script = 'java';
    const args = ['-jar', context.asAbsolutePath('gobpie-0.0.4-SNAPSHOT.jar'), adbSocketPath];

    // Use this for communicating on stdio 
    let serverOptions: ServerOptions = {
        run: {command: script, args: args},
        debug: {command: script, args: args},
    };

    /**
     *   Use this for debugging
     *   let serverOptions = () => {
		const socket = net.connect({ port: 5007 })
		const result: StreamInfo = {
			writer: socket,
			reader: socket
		}
		return new Promise<StreamInfo>((resolve) => {
			socket.on("connect", () => resolve(result))
			socket.on("error", _ =>
				window.showErrorMessage(
					"Failed to connect to the language server. Make sure that the language server is running " +
					"-or- configure the extension to connect via standard IO."))
		})
    }*/

    let clientOptions: LanguageClientOptions = {
        documentSelector: [{scheme: 'file', language: 'c'}],
        synchronize: {
            configurationSection: 'c',
            fileEvents: [workspace.createFileSystemWatcher('**/*.c')]
        }
    };

    // Create the language client and start the client.
    let lc: LanguageClient = new LanguageClient('GobPie', 'GobPie', serverOptions, clientOptions);
    lc.registerFeature(new MagpieBridgeSupport(lc));
    lc.start();

    context.subscriptions.push(vscode.debug.registerDebugAdapterDescriptorFactory('c_adb', new AbstractDebuggingAdapterDescriptorFactory(adbSocketPath)));
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('c_adb', new AbstractDebuggingConfigurationResolver()));
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider('c_adb', new AbstractDebuggingConfigurationProvider(), vscode.DebugConfigurationProviderTriggerKind.Dynamic));
}


export class MagpieBridgeSupport implements DynamicFeature<undefined> {
    constructor(private _client: LanguageClient) {
    }

    messages: RPCMessageType | RPCMessageType[];
    fillInitializeParams?: (params: InitializeParams) => void;

    fillClientCapabilities(capabilities: ClientCapabilities): void {
        capabilities.experimental = {
            supportsShowHTML: true
        }
    }

    initialize(capabilities: ServerCapabilities, documentSelector: DocumentSelector): void {
        this._client.onNotification("magpiebridge/showHTML", (content: string) => {
            this.createWebView(content);
        });
    }

    createWebView(content: string) {
        const columnToShowIn = ViewColumn.Beside;

        if (panel) {
            // If we already have a panel, show it in the target column
            panel.reveal(columnToShowIn);
        } else {
            // Otherwise, create a new panel
            panel = window.createWebviewPanel("Customized Web View", "GobPie", columnToShowIn, {
                retainContextWhenHidden: true,
                enableScripts: true
            });

            // Reset when the current panel is closed
            panel.onDidDispose(
                () => {
                    panel = undefined;
                },
                null,
            );
        }

        panel.webview.html = content;
        panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'action':
                        var httpRequest = new XMLHttpRequest();
                        var url = message.text;
                        httpRequest.open('GET', url);
                        httpRequest.send();
                        return;
                }

            }
        )
    }

    register(message: RPCMessageType, data: RegistrationData<undefined>): void {
    }

    unregister(id: string): void {
    }

    dispose(): void {
    }

}

class AbstractDebuggingAdapterDescriptorFactory implements vscode.DebugAdapterDescriptorFactory {

    constructor(private adbSocketPath: string) {
    }

    async createDebugAdapterDescriptor(session: vscode.DebugSession, executable: vscode.DebugAdapterExecutable | undefined): Promise<vscode.DebugAdapterDescriptor> {
        // TODO: Make sure that GobPie is actually guaranteed to run and create a socket in the workspace folder (in particular multiple workspaces might violate this assumption)
        const socketPath = session.workspaceFolder.uri.path + '/' + this.adbSocketPath;
        try {
            await vscode.workspace.fs.stat(Uri.file(socketPath));
        } catch (e) {
            if (e.code == 'FileNotFound' || e.code == 'ENOENT') {
                throw 'GobPie is not running. Open a C file to start GobPie.'
            } else {
                throw e;
            }
        }
        return new vscode.DebugAdapterNamedPipeServer(socketPath);
    }

}

class AbstractDebuggingConfigurationProvider implements vscode.DebugConfigurationProvider {

    provideDebugConfigurations(folder: WorkspaceFolder | undefined, token?: CancellationToken): DebugConfiguration[] {
        return [{
            type: "c_adb",
            request: "launch",
            name: "C (GobPie Abstract Debugger)"
        }];
    }

}

class AbstractDebuggingConfigurationResolver implements vscode.DebugConfigurationProvider {

    resolveDebugConfiguration(folder: WorkspaceFolder | undefined, debugConfiguration: DebugConfiguration, token?: CancellationToken): DebugConfiguration {
        return {
            type: "c_adb",
            request: "launch",
            name: "C (GobPie Abstract Debugger)",
            ...debugConfiguration
        };
    }

}
