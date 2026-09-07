const readline = require('readline');

const SERVER_INFO = {
    name: 'dsh-java-demo-plugin',
    version: '1.0.0',
};

const tools = [
    {
        name: 'demo_echo',
        description: 'Echoes the supplied text back to the agent.',
        inputSchema: {
            type: 'object',
            properties: {
                text: { type: 'string', description: 'Text to echo' },
            },
            required: ['text'],
        },
    },
];

function writeMessage(message) {
    process.stdout.write(`${JSON.stringify(message)}\n`);
}

function callTool(name, args) {
    if (name !== 'demo_echo') {
        throw new Error(`Unknown tool: ${name}`);
    }

    const text = typeof args?.text === 'string' ? args.text : '';
    if (!text) {
        throw new Error('text is required');
    }

    return {
        content: [{ type: 'text', text }],
    };
}

const jsonRpc = readline.createInterface({
    input: process.stdin,
    terminal: false,
});

jsonRpc.on('line', (line) => {
    if (!line.trim()) {
        return;
    }

    let request;
    try {
        request = JSON.parse(line);
    } catch (error) {
        console.error(`Invalid JSON-RPC input: ${error.message}`);
        return;
    }

    const response = {
        jsonrpc: '2.0',
        id: request.id,
    };

    try {
        if (request.method === 'initialize') {
            response.result = {
                protocolVersion: request.params?.protocolVersion || '2024-11-05',
                serverInfo: SERVER_INFO,
            };
        } else if (request.method === 'tools/list') {
            response.result = { tools };
        } else if (request.method === 'tools/call') {
            response.result = callTool(request.params?.name, request.params?.arguments);
        } else {
            throw new Error(`Unknown JSON-RPC method: ${request.method}`);
        }
    } catch (error) {
        response.error = {
            code: -32601,
            message: error.message,
        };
    }

    writeMessage(response);
});

console.error('dsh-demo-plugin started');
