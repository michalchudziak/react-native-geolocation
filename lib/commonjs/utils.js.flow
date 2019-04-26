/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow strict
 */

'use strict';

/**
 * Small utility that can be used as an error handler. You cannot just pass
 * `console.error` as a failure callback - it's not properly bound.  If passes an
 * `Error` object, it will print the message and stack.
 */
const logError = function(...args: $ReadOnlyArray<mixed>) {
  if (args.length === 1 && args[0] instanceof Error) {
    const err = args[0];
    console.error('Error: "' + err.message + '".  Stack:\n' + err.stack);
  } else {
    console.error.apply(console, args);
  }
};

/**
 * Similar to invariant but only logs a warning if the condition is not met.
 * This can be used to log issues in development environments in critical
 * paths. Removing the logging code for production environments will keep the
 * same logic and follow the same code paths.
 */
const warning = __DEV__
  ? function(condition: boolean, format: string, ...args: any[]) {
      if (format === undefined) {
        throw new Error(
          '`warning(condition, format, ...args)` requires a warning ' +
            'message argument',
        );
      }
      if (!condition) {
        var argIndex = 0;
        var message =
          'Warning: ' + format.replace(/%s/g, () => args[argIndex++]);
        if (typeof console !== 'undefined') {
          console.error(message);
        }
        try {
          // --- Welcome to debugging React ---
          // This error was thrown as a convenience so that you can use this stack
          // to find the callsite that caused this warning to fire.
          throw new Error(message);
        } catch (x) {}
      }
    }
  : function(...args: any[]) {};

module.exports = {
  logError,
  warning,
};
