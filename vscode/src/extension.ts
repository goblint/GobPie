'use strict';
import {ExtensionContext, workspace} from 'vscode';
import {LanguageClient, LanguageClientOptions, ServerOptions} from 'vscode-languageclient';

export function activate(context: ExtensionContext) {
    let script = 'java';
    let args = ['-jar', context.asAbsolutePath('gobpie-0.0.2-SNAPSHOT.jar')];

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
    lc.start();
}

