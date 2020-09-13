Date.prototype.format = function (fmt) {
    var o = {
        'M+': this.getMonth() + 1, //月份
        'd+': this.getDate(), //日
        'H+': this.getHours(), //小时
        'm+': this.getMinutes(), //分
        's+': this.getSeconds(), //秒
        'q+': Math.floor((this.getMonth() + 3) / 3), //季度
        'S': this.getMilliseconds() //毫秒
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp('(' + k + ')').test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)));
    return fmt;
};

function appendScriptType($scope) {
    $scope.scriptTypeList = [{name: 'shell批处理', value: 0}, {name: 'spark流处理', value: 1}, {name: 'spark批处理', value: 2},
        {name: 'flink流处理', value: 3}, {name: 'flink批处理', value: 4}];
    $scope.scriptTypeMap = {};
    $scope.scriptTypeList.forEach(function (item) {
        $scope.scriptTypeMap[item.value] = item;
    });
}

function appendSchedulingType($scope) {
    $scope.schedulingTypeList = [{name: '批处理', value: 0}, {name: '流处理', value: 1}];
    $scope.schedulingTypeMap = {};
    $scope.schedulingTypeList.forEach(function (item) {
        $scope.schedulingTypeMap[item.value] = item;
    });
}

function appendCmdStatus($scope) {
    $scope.cmdStatusList = [{name: '未开始', value:1, style: 'info'}, {name: '执行中', value: 2, style: 'warning'},
        {name: '已完成', value: 3, style: 'success'}, {name: '运行超时', value: 4, style: 'danger'}, {name: '运行失败', value: 5, style: 'danger'}];
    $scope.cmdStatusMap = {};
    $scope.cmdStatusList.forEach(function (item) {
        $scope.cmdStatusMap[item.value] = item;
    });
}

function appendYarnJobState($scope) {
    $scope.yarnJobStateList = [{name: '初始状态', value: 'NEW', style: 'warning'}, {name: '作业提交请求', value: 'NEW_SAVING', style: 'warning'}, {name: '提交成功', value: 'SUBMITTED', style: 'warning'},
        {name: '等待调度', value: 'ACCEPTED', style: 'warning'}, {name: '运行中', value: 'RUNNING', style: 'success'},{name: '已完成', value: 'FINISHED', style: 'success'},
        {name: '手动停止', value: 'KILLED', style: 'danger'}, {name: '运行失败', value: 'FAILED', style: 'danger'}
    ];
    $scope.yarnJobStateMap = {};
    $scope.yarnJobStateList.forEach(function (item) {
        $scope.yarnJobStateMap[item.value] = item;
    });
}

function appendBooleanType($scope) {
    $scope.booleanTypeList = [{name: '否', value: false, style: 'danger'}, {name: '是', value: true, style: 'success'}];
    $scope.booleanTypeMap = {};
    $scope.booleanTypeList.forEach(function (item) {
        $scope.booleanTypeMap[item.value] = item;
    });
}

function appendCycle($scope) {
    $scope.cycleList = [{name: '分钟', value: 1}, {name: '小时',value: 2}, {name: '天', value: 3},
        {name: '周', value: 4}];
    $scope.cycleMap = {};
    $scope.cycleList.forEach(function (item) {
        $scope.cycleMap[item.value] = item;
    });
}

function appendWeek($scope) {
    $scope.weekList = [{name: '星期天', value: '1'}, {name: '星期一', value: '2'}, {name: '星期二', value: '3'},
        {name: '星期三', value: '4'}, {name: '星期四', value: '5'}, {name: '星期五', value:'6'},
        {name: '星期六', value: '7'}];
    $scope.weekMap = {};
    $scope.weekList.forEach(function (item) {
        $scope.weekMap[item.value] = item;
    });
}

function appendHour($scope) {
    $scope.hourList = [{name: '0点', value: 0}, {name: '1点', value: 1}, {name: '2点', value: 2},
        {name: '3点', value: 3}, {name: '4点', value: 4}, {name: '5点', value: 5},
        {name: '6点', value: 6}, {name: '7点', value: 7}, {name: '8点', value: 8},
        {name: '9点', value: 9}, {name: '10点', value: 10}, {name: '11点', value: 11},
        {name: '12点', value: 12}, {name: '13点', value: 13}, {name: '14点', value: 14},
        {name: '15点', value: 15}, {name: '16点', value: 16}, {name: '17点', value: 17},
        {name: '18点', value: 18}, {name: '19点', value: 19}, {name: '20点', value: 20},
        {name: '21点', value: 21}, {name: '22点', value: 22}, {name: '23点', value: 23}];
    $scope.hourMap = {};
    $scope.hourList.forEach(function (item) {
        $scope.hourMap[item.value] = item;
    });
}

function appendMinute($scope) {
    $scope.minuteList = [{name: '0分', value: 0}, {name: '1分', value: 1}, {name: '2分', value: 2},
        {name: '3分', value: 3}, {name: '4分', value: 4}, {name: '5分', value: 5},
        {name: '6分', value: 6}, {name: '7分', value: 7}, {name: '8分', value: 8},
        {name: '9分', value: 9}, {name: '10分', value: 10}, {name: '11分', value: 11},
        {name: '12分', value: 12}, {name: '13分', value: 13}, {name: '14分', value: 14},
        {name: '15分', value: 15}, {name: '16分', value: 16}, {name: '17分', value: 17},
        {name: '18分', value: 18}, {name: '19分', value: 19}, {name: '20分', value: 20},
        {name: '21分', value: 21}, {name: '22分', value: 22}, {name: '23分', value: 23},
        {name: '24分', value: 24}, {name: '25分', value: 25}, {name: '26分', value: 26},
        {name: '27分', value: 27}, {name: '28分', value: 28}, {name: '29分', value: 29},
        {name: '30分', value: 30}, {name: '31分', value: 31}, {name: '32分', value: 32},
        {name: '33分', value: 33}, {name: '34分', value: 34}, {name: '35分', value: 35},
        {name: '36分', value: 36}, {name: '37分', value: 37}, {name: '38分', value: 38},
        {name: '39分', value: 39}, {name: '40分', value: 40}, {name: '41分', value: 41},
        {name: '42分', value: 42}, {name: '43分', value: 43}, {name: '44分', value: 44},
        {name: '45分', value: 45}, {name: '46分', value: 46}, {name: '47分', value: 47},
        {name: '48分', value: 48}, {name: '49分', value: 49}, {name: '50分', value: 50},
        {name: '51分', value: 51}, {name: '52分', value: 52}, {name: '53分', value: 53},
        {name: '54分', value: 54}, {name: '55分', value: 55}, {name: '56分', value: 56},
        {name: '57分', value: 57}, {name: '58分', value: 58}, {name: '59分', value: 59}];
    $scope.minuteMap = {};
    $scope.minuteList.forEach(function (item) {
        $scope.minuteMap[item.value] = item;
    });
}

function defineSort($scope) {
    //排序
    $scope.sort = function(title, asc) {
        if (title !== $scope.title) {
            asc = undefined;
        }
        if (asc === false) {
            $scope.title = null;
            $scope.asc = null;
        } else {
            $scope.title = title;
            $scope.asc = !asc;
        }
    };
}

function removeItem(url, item, $http, afterRemoveItem) {
    swal({
        title: '确认删除？',
        type: 'question',
        showConfirmButton: true,
        showCancelButton: true
    }).then(function (result) {
        if (result.value) {
            $http.post(url + '?id=' + item.id)
                .success(function () {
                    swal({
                        title: '删除成功',
                        type: 'success',
                        showConfirmButton: false,
                        timer: 1500
                    });
                    if (afterRemoveItem) {
                        afterRemoveItem()
                    }
                })
        }
    });
}

function getAuthUser($scope, $http) {
    $scope.userList = [];
    $scope.userMap = {};
    $http.get($contextPath + 'auth/user/getall.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.userList.push(item);
                $scope.userMap[item.id] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getCluster($scope, $http, callback) {
    $scope.clusterList = [];
    $scope.clusterMap = {};
    $http.get($contextPath + 'cluster/getall.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.clusterList.push(item);
                $scope.clusterMap[item.id] = item;
            });
            if (callback) {
                callback();
            }
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getAgent($scope, $http) {
    $scope.agentList = [];
    $scope.agentMap = {};
    $http.get($contextPath + 'cluster/agent/getall.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.agentList.push(item);
                $scope.agentMap[item.id] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getClusterUser($scope, $http) {
    $scope.clusterUserList = [];
    $scope.clusterUserMap = {};
    $http.get($contextPath + 'cluster/cluster_user/getall.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.clusterUserList.push(item);
                $scope.clusterUserMap[item.uid + '_' + item.clusterId] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getScript($scope, $http, callback) {
    $scope.scriptList = [];
    $scope.scriptMap = {};
    $http.get($contextPath + 'script/getall.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.scriptList.push(item);
                $scope.scriptMap[item.id] = item;
            });
            if (callback) {
                callback();
            }
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getClusterSync($scope) {
    $scope.clusterList = [];
    $scope.clusterMap = {};
    $.ajax({
        url: $contextPath + 'cluster/getall.api',
        async: false,
        type: 'GET',
        contentType: 'application/json',
        success: function (data) {
            if (data.code === 0) {
                data.content.forEach(function (item) {
                    $scope.clusterList.push(item);
                    $scope.clusterMap[item.id] = item;
                })
            } else {
                swal({
                    title: '后台接口异常，请联系管理员',
                    type: 'error',
                    showConfirmButton: true
                });
            }
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        }
    });
}

function getComputeFrameworkVersionSync($scope) {
    $scope.sparkVersionList = [];
    $scope.sparkVersionMap = {};
    $scope.flinkVersionList = [];
    $scope.flinkVersionMap = [];
    $.ajax({
        url: $contextPath + 'cluster/compute_framework/getall.api',
        async: false,
        type: 'GET',
        contentType: 'application/json',
        success: function (data) {
            if (data.code === 0) {
                data.content.forEach(function (item) {
                    if (item.type === 'Spark') {
                        $scope.sparkVersionList.push(item);
                        $scope.sparkVersionMap[item.command] = item;
                    } else if (item.type === 'Flink') {
                        $scope.flinkVersionList.push(item);
                        $scope.flinkVersionMap[item.command] = item;
                    }
                });
            } else {
                swal({
                    title: '后台接口异常，请联系管理员',
                    type: 'error',
                    showConfirmButton: true
                });
            }
            if ($scope.sparkVersionList.length === 0) {
                var spark = {
                    version: 'default',
                    command: 'spark-submit'
                };
                $scope.sparkVersionList.push(spark);
                $scope.sparkVersionMap[spark.command] = spark;
            }
            if ($scope.flinkVersionList.length === 0) {
                var flink = {
                    version: 'default',
                    command: 'flink'
                };
                $scope.flinkVersionList.push(flink);
                $scope.flinkVersionMap[flink.command] = flink;
            }
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        }
    });
}
