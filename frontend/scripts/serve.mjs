// Run Angular in this process using the port loaded from .env.local.
process.argv.splice(2, 0, 'serve', '--port', process.env.FRONTEND_PORT || '6100');
await import('../node_modules/@angular/cli/bin/ng.js');
