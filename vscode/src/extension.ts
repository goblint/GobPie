'use strict';
import * as net from 'net';
import * as path from 'path';

import {  workspace, window, ExtensionContext } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo } from 'vscode-languageclient';

export function activate(context: ExtensionContext) {
    let script = 'java';
    let args = ['-jar',context.asAbsolutePath('tutorial2-0.0.1-SNAPSHOT.jar'), context.asAbsolutePath('preparedResults.json')];
    
    // Use this for communicating on stdio 
    let serverOptions: ServerOptions = {
        run : { command: script, args: args },
        debug: { command: script, args: args} ,
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
					"Failed to connect to TaintBench language server. Make sure that the language server is running " +
					"-or- configure the extension to connect via standard IO."))
		})
    }*/
    
    let clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'java' }],
        synchronize: {
            configurationSection: 'java',
            fileEvents: [ workspace.createFileSystemWatcher('**/*.java') ]
        }
    };
    
    // Create the language client and start the client.
    let lc : LanguageClient = new LanguageClient('Tutorial2','Tutorial2', serverOptions, clientOptions);
    lc.start();
}

