'use strict';

/**
 * @ngdoc module
 * @name angular-bootstrap-select.extra
 * @description
 * Angular bootstrap-select extra.
 */

angular.module('angular-bootstrap-select.extra', [])
    .directive('dropdownToggle', [dropdownToggleDirective])
    .directive('dropdownClose', [dropdownCloseDirective]);

/**
 * @ngdoc directive
 * @name dropdownToggle
 * @restrict ACE
 *
 * @description
 * This extra directive provide dropdown toggle specifically to bootstrap-select without loading bootstrap.js.
 *
 * @usage
 * ```html
 * <div class="dropdown-toggle">
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </div>
 *
 * <div dropdown-toggle>
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </div>
 *
 * <dropdown-toggle>
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </dropdown-toggle>
 * ```
 */

function dropdownToggleDirective() {
    return {
        restrict: 'ACE',
        priority: 101,
        link: function (scope, element, attrs) {
            var toggleFn = function (e) {
                var parent = angular.element(this).parent();

                angular.element('.bootstrap-select.open', element)
                    .not(parent)
                    .removeClass('open');

                parent.toggleClass('open');
            };

            element.on('click.bootstrapSelect', '.dropdown-toggle', toggleFn);

            scope.$on('$destroy', function () {
                element.off('.bootstrapSelect');
            });
        }
    };
}

/**
 * @ngdoc directive
 * @name dropdownClear
 * @restrict ACE
 *
 * @description
 * This extra directive provide the closing of ALL open dropdowns clicking away
 *
 * @usage
 * ```html
 * <div class="dropdown-close">
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </div>
 *
 * <div dropdown-close>
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </div>
 *
 * <dropdown-close>
 *   <select class="selectpicker">
 *      <option value="">Select one</option>
 *      <option>Mustard</option>
 *      <option>Ketchup</option>
 *      <option>Relish</option>
 *   </select>
 * </dropdown-close>
 * ```
 */

function dropdownCloseDirective() {
    return {
        restrict: 'ACE',
        priority: 101,
        link: function (scope, element, attrs) {
            var hideFn = function (e) {
                var parent = e.target.tagName !== 'A' && angular.element(e.target).parents('.bootstrap-select');

                angular.element('.bootstrap-select.open', element)
                    .not(parent)
                    .removeClass('open');
            };

            angular.element(document).on('click.bootstrapSelect', hideFn);

            scope.$on('$destroy', function () {
                angular.element(document).off('.bootstrapSelect');
            });
        }
    };
}

/**
 * @ngdoc module
 * @name angular-bootstrap-select
 * @description
 * Angular bootstrap-select.
 */

angular.module('angular-bootstrap-select', [])
    .directive('selectpicker', ['$parse', '$timeout', selectpickerDirective]);

/**
 * @ngdoc directive
 * @name selectpicker
 * @restrict A
 *
 * @param {object} selectpicker Directive attribute to configure bootstrap-select, full configurable params can be found in [bootstrap-select docs](http://silviomoreto.github.io/bootstrap-select/).
 * @param {string} ngModel Assignable angular expression to data-bind to.
 *
 * @description
 * The selectpicker directive is used to wrap bootstrap-select.
 *
 * @usage
 * ```html
 * <select selectpicker ng-model="model">
 *   <option value="">Select one</option>
 *   <option>Mustard</option>
 *   <option>Ketchup</option>
 *   <option>Relish</option>
 * </select>
 *
 * <select selectpicker="{dropupAuto:false}" ng-model="model">
 *   <option value="">Select one</option>
 *   <option>Mustard</option>
 *   <option>Ketchup</option>
 *   <option>Relish</option>
 * </select>
 * ```
 */

function selectpickerDirective($parse, $timeout) {
    return {
        restrict: 'A',
        priority: 1000,
        link: function (scope, element, attrs) {
            function refresh(newVal) {
                scope.$applyAsync(function () {
                    if (attrs.ngOptions && /track by/.test(attrs.ngOptions)) element.val(newVal);
                    element.selectpicker('refresh');
                });
            }

            attrs.$observe('spTheme', function (val) {
                $timeout(function () {
                    element.data('selectpicker').$button.removeClass(function (i, c) {
                        return (c.match(/(^|\s)?btn-\S+/g) || []).join(' ');
                    });
                    element.selectpicker('setStyle', val);
                });
            });

            $timeout(function () {
                element.selectpicker($parse(attrs.selectpicker)());
                element.selectpicker('refresh');
            });

            if (attrs.ngModel) {
                scope.$watch(attrs.ngModel, refresh, true);
            }

            if (attrs.ngDisabled) {
                scope.$watch(attrs.ngDisabled, refresh, true);
            }

            scope.$on('$destroy', function () {
                $timeout(function () {
                    element.selectpicker('destroy');
                });
            });
        }
    };
}
