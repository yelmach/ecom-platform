const path = require('node:path');

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
    ],
    client: {
      clearContext: false,
    },
    reporters: ['progress', 'kjhtml', 'junit', 'coverage'],
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--headless', '--disable-gpu', '--disable-dev-shm-usage'],
      },
    },
    junitReporter: {
      outputDir: 'reports/junit',
      outputFile: 'frontend-tests.xml',
      useBrowserName: false,
    },
    coverageReporter: {
      dir: path.join(__dirname, 'coverage/frontend'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'lcov' }, { type: 'text-summary' }],
    },
    restartOnFileChange: false,
    singleRun: true,
  });
};
