var dateTimeFormat1 = 'YYYY/MM/DD hh:mm:ss';
var dateFormat1 = 'YYYY/MM/DD';
var dateTimeFormat2 = 'yyyy/MM/dd hh:mm:ss';
var dateFormat2 = 'yyyy/MM/dd';
var dateFormat3 = 'yyyy-MM-dd';
var dateTimeFormat3 = 'YYYY-MM-DD hh:mm:ss';

var loadJS = function (src) {
    var script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', src);
    script.setAttribute('charset', "utf-8");
    document.getElementsByTagName('head')[0].appendChild(script);
};

var loadCSS = function (src) {
    var script = document.createElement('link');
    script.setAttribute('href', src);
    script.setAttribute('type', 'text/css');
    script.setAttribute('rel', 'stylesheet');
    document.getElementsByTagName('head')[0].appendChild(script);
};

function registerPage(app) {
    if (app.RegisterPage) return;
    app.RegisterPage = true;
    loadCSS($contextPath + 'libs/angular-1.3.9/cusFile/mypage.css');
    app.directive('mypage', function () {
        var directive = {};
        directive.restrict = 'E';
        directive.replace = true;
        directive.transclude = false;
        directive.templateUrl = $contextPath + 'libs/angular-1.3.9/cusFile/mypage.html';
        directive.scope = {
            model: '=ngModel'
        };
        directive.controller = function ($scope, $http) {
            $scope.model.mypage = $scope;
            $scope.pageNumbers = [0];
            $scope.pageSizes = [10, 20, 50, 100];
            $scope.query = $scope.model.query || {pageNo: 1, pageSize: 10};
            $scope.reload = function () {
                if (!$scope.query.pageNo) {
                    $scope.query.pageNo = 1;
                }
                if (!$scope.query.pageSize) {
                    $scope.query.pageSize = 10;
                }
                $scope.pagination = {
                    totalPages: 0,
                    totalElements: 0,
                    number: $scope.query.pageNo - 1,
                    size: $scope.query.pageSize,
                    first: true,
                    last: true
                };
                $http.post($scope.model.listUrl, $scope.query)
                    .success(function (data) {
                        $scope.pagination = data;
                        $scope.pageNumbers = [];
                        if (!$scope.pagination.first) {
                            $scope.pageNumbers.push($scope.pagination.number - 1);
                        }
                        $scope.pageNumbers.push($scope.pagination.number);
                        if (!$scope.pagination.last) {
                            $scope.pageNumbers.push($scope.pagination.number + 1);
                        }
                        $scope.model.items = data.content;
                        $scope.model.itemData = data;
                        if ($scope.model.reloadAfter) {
                            $scope.model.reloadAfter($scope.model.items);
                        }
                });
            };
            $scope.toFirst = function () {
                $scope.query.pageNo = 1;
                $scope.reload();
            };
            $scope.toLast = function () {
                $scope.query.pageNo = $scope.pagination.totalPages;
                $scope.reload();
            };
            $scope.toNext = function () {
                if (!$scope.pagination.last) {
                    $scope.query.pageNo += 1;
                    $scope.reload();
                }
            };
            $scope.toPrevious = function () {
                if (!$scope.pagination.first) {
                    $scope.query.pageNo -= 1;
                    $scope.reload();
                }
            };
            $scope.toPage = function (page) {
                $scope.query.pageNo = parseInt(page);
                $scope.reload();
            };
            $scope.$watch('query.pageSize', function (v0, v1) {
                if(v0 != v1) {
                    $scope.reload();
                }
            });

            if (!$scope.model.noInit) {
                $scope.reload();
            }

        };
        return directive;
    });
}

var registerDateRange = function (app, startId, endId) {
    var start = {
        elem: '#' + startId,
        format: dateTimeFormat3,
        min: laydate.now(),
        max: '2099-06-16 23:59:59', //最大日期
        istime: true,
        istoday: true,
        choose: function(datas){
            end.min = datas; //开始日选好后，重置结束日的最小日期
            end.start = datas //将结束日的初始值设定为开始日
        }
    };
    var end = {
        elem: '#' + endId,
        format: dateTimeFormat3,
        max: '2099-06-16 23:59:59',
        istime: true,
        istoday: true,
        choose: function(datas){
            start.max = datas; //结束日选好后，重置开始日的最大日期
        }
    };

    laydate(start);
    laydate(end);
};

var _ID_ = 1;

function registerDateRangePicker(app) {
    if (app.RegisterDateRangePicker) return;
    app.RegisterDateRangePicker = true;
    loadCSS($contextPath + 'libs/angular-1.3.9/cusFile/mydaterange.css');
    app.directive('mydaterange', function () {
        var directive = {};
        directive.restrict = 'E';
        directive.replace = true;
        directive.transclude = false;
        directive.templateUrl = $contextPath + 'libs/angular-1.3.9/cusFile/mydaterange.html';
        directive.scope = {
            startTime: '=ngStart',
            endTime: '=ngEnd',
            initValue: '@'
        };
        directive.controller = function ($scope, $http, $sce, $element) {
            var e = $element;
            if (!$(e).attr('id')) {
                _ID_++;
                var id = 'laydate_' + _ID_;
                $(e).attr('id', id);
                var startId = id + '_start';
                var endId = id + '_end';
                $(e).find('input[name="start"]').attr('id', startId);
                $(e).find('input[name="end"]').attr('id', endId);
                var startOptions = {
                    elem: '#' + startId,
                    format: dateTimeFormat3,
                    istime: true,
                    istoday: true,
                    isclear: true,
                    choose: function (v) {
                        endOptions.min = v;
                        $scope.$apply(function () {
                            $scope.startTime = v;
                        });
                    }
                };
                var endOptions = {
                    elem: '#' + endId,
                    format: dateTimeFormat3,
                    istime: true,
                    istoday: true,
                    isclear: true,
                    choose: function (v) {
                        startOptions.max = v;
                        $scope.$apply(function () {
                            $scope.endTime = v;
                        });
                    }
                };
                laydate(startOptions);
                laydate(endOptions);
                //init model
                if ($scope.initValue) {
                    $(e).find('select').val($scope.initValue);
                    setModelValue($scope, $scope.initValue);
                } else {
                    $(e).find('select').val('');
                    setModelValue($scope, '');
                }

                function isCustom() {
                    return $(e).find('select').val() == 'custom';
                }

                function setCustomVisible() {
                    if (isCustom()) {
                        $('#' + startId).val($scope.startTime);
                        $('#' + startId).show();
                        $('#' + endId).val($scope.endTime);
                        $('#' + endId).show();
                    } else {
                        $('#' + startId).hide();
                        $('#' + endId).hide();
                    }
                }

                setCustomVisible();

                $(e).find('select').change(function () {
                    setCustomVisible();
                    if (isCustom()) {
                        return;
                    }
                    var v = $(e).find('select').val();
                    $scope.$apply(function () {
                        setModelValue($scope, v);
                    });
                });

                function setModelValue($scope, v) {

                    var startT = '';
                    var endT = '';

                    if (v == '1') {
                        var n = new Date().format(dateFormat3);
                        startT = endT = n;
                    } else if (v == '-1') {
                        var n = new Date(new Date().getTime() - 24 * 3600000).format(dateFormat3);
                        startT = endT = n;
                    } else if (v == '7') {
                        var weekDay = (new Date().getDay() + 6) % 7;
                        startT = new Date(new Date().getTime() - weekDay * 24 * 3600000).format(dateFormat3);
                        endT = new Date(new Date().getTime() + (6 - weekDay) * 24 * 3600000).format(dateFormat3);
                    } else if (v == '-7') {
                        var weekDay = (new Date().getDay() + 6) % 7;
                        startT = new Date(new Date().getTime() - (7 + weekDay) * 24 * 3600000).format(dateFormat3);
                        endT = new Date(new Date().getTime() + (-1 - weekDay) * 24 * 3600000).format(dateFormat3);
                    } else if (v == '30') {
                        startT = new Date(new Date().getTime() - (new Date().getDate() - 1) * 24 * 3600000).format(dateFormat3);
                        var nn = new Date(new Date().getTime() + (40 - new Date().getDate()) * 24 * 3600000);
                        endT = new Date(nn.getTime() - nn.getDate() * 24 * 3600000).format(dateFormat3);
                    } else if (v == '-30') {
                        var n = new Date();
                        var pn = new Date(n.getTime() - n.getDate() * 24 * 3600000);
                        endT = pn.format(dateFormat3);
                        startT = new Date(pn.getTime() - (pn.getDate() - 1) * 24 * 3600000).format(dateFormat3);
                    } else if (v == '') {   //时间不限
                        $scope.startTime = '';
                        $scope.endTime = '';
                        return;
                    } else {        //自定义，默认原有值
                        return;
                    }
                    startT = startT + ' 00:00:00';
                    endT = endT + ' 23:59:59';

                    $scope.startTime = startT;
                    $scope.endTime = endT;
                }
            }
        };
        return directive;
    });
}

function registerHttpInterceptor(app) {
    app.factory('httpInterceptor', ['$q', '$injector', function ($q, $injector) {
        return {
            response: function (response) {
                if (typeof response.data === 'string' && response.data.indexOf('<title>登录 - Big Whale</title>') !== -1) {
                    swal({
                        title: '登录超时',
                        text: '请重新登录',
                        type: 'error',
                        showConfirmButton: true
                    }).then(function () {
                        window.location.href = '/login.html'
                    });
                    return response;
                } else if (typeof response.data === 'object') {
                    if (response.data.code === 0) {
                        response.data = response.data.content;
                        return response;
                    } else if (response.data.code === -999) {
                        swal({
                            title: '后台接口异常，请联系管理员',
                            type: 'error',
                            showConfirmButton: true
                        });
                        return $q.reject(response);
                    } else {
                        swal({
                            title: response.data.msg || '操作失败',
                            type: 'warning',
                            showConfirmButton: true
                        });
                        return $q.reject(response);
                    }
                }
                return response;
            }
        };
    }]);
    app.config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('httpInterceptor');
    }]);
}
