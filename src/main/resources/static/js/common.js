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
    $scope.booleanTypeList = [{name: '是', value: true, style: 'success'}, {name: '否', value: false, style: 'danger'}];
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

function removeItem(url, item, $http, callback) {
    swal({
        title: '确认删除？',
        type: 'question',
        showConfirmButton: true,
        showCancelButton: true
    }).then(function (result) {
        if (result.value) {
            $('#' + item.id + '_removeBtn').attr('disabled', 'true');
            $http.post(url, {
                id: item.id
            }).success(function () {
                $('#' + item.id + '_removeBtn').removeAttr('disabled');
                swal({
                    title: '删除成功',
                    type: 'success',
                    showConfirmButton: false,
                    timer: 1500
                }).then(function () {
                    if (callback) {
                        callback()
                    }
                });
            }).error(function () {
                $('#' + item.id + '_removeBtn').removeAttr('disabled');
            })
        }
    });
}

function getSchedule($scope, $http) {
    $scope.scheduleList = [];
    $scope.scheduleMap = {};
    $http.get($contextPath + 'schedule/all.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.scheduleList.push(item);
                $scope.scheduleMap[item.id] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getStream($scope, $http) {
    $scope.streamList = [];
    $scope.streamMap = {};
    $http.get($contextPath + 'stream/all.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.streamList.push(item);
                $scope.streamMap[item.id] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getAuthUser($scope, $http) {
    $scope.userList = [];
    $scope.userMap = {};
    $http.get($contextPath + 'auth/user/all.api')
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
    $http.get($contextPath + 'cluster/all.api')
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

function getClusterUser($scope, $http) {
    $scope.clusterUserList = [];
    $scope.clusterUserMap = {};
    $http.get($contextPath + 'cluster/cluster_user/all.api')
        .success(function (data) {
            data.forEach(function (item) {
                $scope.clusterUserList.push(item);
                $scope.clusterUserMap[item.userId + '_' + item.clusterId] = item;
            });
            setTimeout(function () {
                $('.selectpicker').selectpicker('refresh');
            }, 100);
        });
}

function getAgent($scope, $http) {
    $scope.agentList = [];
    $scope.agentMap = {};
    $http.get($contextPath + 'cluster/agent/all.api')
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

function getComputeFrameworkVersionSync($scope) {
    $scope.sparkVersionList = [];
    $scope.sparkVersionMap = {};
    $scope.flinkVersionList = [];
    $scope.flinkVersionMap = [];
    $.ajax({
        url: $contextPath + 'cluster/compute_framework/all.api',
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

function initScriptEditProp($scope, $timeout) {

    //多个空格转成一个空格
    String.prototype.resetBlank = function () {
        var regEx = /\s+/g;
        return this.replace(regEx, ' ');
    };
    
    var getScript = function () {
        return $scope.editItem.script || $scope.editItem;
    };

    var initUpload = function () {
        $('#advancedDropzone').dropzone({
            url: '../hdfs/upload',
            autoProcessQueue: false,
            // Events
            addedfile: function(file) {
                if (!file.name.endsWith('.jar') && !file.name.endsWith('.py')) {
                    swal('程序包检验错误', '文件格式错误', 'error');
                    this.removeFile(file);
                    return;
                }
                $('#advancedDropzone').attr('disabled', 'true');
                var $dropzoneFiletable = $('#dropzone_filetable');
                //先清除子元素
                $dropzoneFiletable.empty();
                var $el = $(
                    '<div class="col-sm-6">' +
                    '<div class="progress progress-striped">' +
                    '<div class="progress-bar progress-bar-warning"></div>' +
                    '</div>' +
                    '</div>' +
                    '<div class="col-sm-6">' +
                    '<span>上传中...</span>' +
                    '</div>');
                $dropzoneFiletable.append($el);
                file.fileEntryTd = $el;
                file.progressBar = $el.find('.progress-bar');
                var dropzone = this;
                $timeout(function () {
                    dropzone.processQueue();
                }, 50);
            },
            sending:function(file, xhr, formData){
                if (getScript().clusterId) {
                    formData.append('clusterId', getScript().clusterId);
                }
            },
            uploadprogress: function(file, progress) {
                file.progressBar.width(progress + '%');
            },
            success: function(file, data) {
                $("#advancedDropzone").removeAttr('disabled');
                if (data.code === 0) {
                    file.fileEntryTd.find('span:last').html('<span class="text-success">成功</span>');
                    file.progressBar.removeClass('progress-bar-warning').addClass('progress-bar-success');
                    getScript().jarPath_ = data.content;
                    $scope.parseVisual(getScript());
                } else {
                    file.fileEntryTd.find('span:last').html('<span class="text-danger">失败</span>');
                    file.progressBar.removeClass('progress-bar-warning').addClass('progress-bar-danger');
                    getScript().jarPath_ = '';
                    if (data.code === -999) {
                        swal({
                            title: '后台接口异常，请联系管理员',
                            type: 'error',
                            showConfirmButton: true
                        });
                    } else {
                        swal({
                            title: data.msg || "操作失败",
                            type: 'warning',
                            showConfirmButton: true
                        });
                    }
                }
            },
            error: function(file, data) {
                $('#advancedDropzone').removeAttr('disabled');
                file.fileEntryTd.find('span:last').html('<span class="text-danger">失败</span>');
                file.progressBar.removeClass('progress-bar-warning').addClass('progress-bar-red');
                getScript().jarPath_ = '';
                swal({
                    title: '后台接口异常，请联系管理员',
                    type: 'error',
                    showConfirmButton: true
                });
            }
        });
    };
    var $a_visual = $('a[href="#visual"]');
    var $a_text = $('a[href="#text"]');
    var sparkArgKey = {
        'proxy-user': 'proxy_user',
        'queue': 'queue',
        'driver-memory': 'driver_memory',
        'driver-cores': 'driver_cores',
        'executor-memory': 'executor_memory',
        'num-executors': 'num_executors',
        'executor-cores': 'executor_cores',
        'name': 'name',
        'class': 'class',
        'master': 'master',
        'deploy-mode': 'deploy_mode'
    };
    var flinkArgKey = {
        'yjm': 'yjm',
        'ytm': 'ytm',
        'yn': 'yn',
        'ys': 'ys',
        'ynm': 'ynm',
        'c': 'c',
        'p': 'p',
        'm': 'm',
        'yqu': 'yqu'
    };
    var parseMemory = function (present) {
        present = present.toUpperCase();
        var val;
        if (present.indexOf('M') !== -1) {
            val = parseInt(present.substring(0, present.indexOf('M')));
        } else if (present.indexOf('G') !== -1) {
            val = parseInt(present.substring(0, present.indexOf('G'))) * 1024;
        } else {
            val = parseInt(present);
        }
        return val;
    };

    $a_visual.on('show.bs.tab', function(e) {
        //shell类型脚本禁用可视化
        if ($a_visual.parent().hasClass('disabled')) {
            e.preventDefault();
            return;
        }
        getScript().editMode_ = 'visual';
        $scope.parseText(getScript());
        $timeout(function() {
            $('[data-toggle="tooltip"]').tooltip();
        }, 100);
    });
    $a_text.on('show.bs.tab', function() {
        getScript().editMode_ = 'text';
        $scope.parseVisual(getScript());
    });

    $scope.Spark = {
        'createDefault': function () {
            return {
                '$command': $scope.sparkVersionList[0]['command'],
                'master': 'yarn',
                'deploy_mode': 'cluster',
                'driver_memory': '512M',
                'driver_cores': '1',
                'executor_memory': '1024M',
                'num_executors': '1',
                'executor_cores': '1',
                'spark_other_args': '--conf spark.yarn.submit.waitAppCompletion=false',
                '$total_memory': this['$cal_total_memory'],
                '$total_cores': this['$cal_total_cores']
            };
        },
        'createEmpty': function () {
            return {
                '$total_memory': this['$cal_total_memory'],
                '$total_cores': this['$cal_total_cores']
            }
        },
        '$cal_total_memory': function () {
            var driver_memory_str = this['driver_memory'] ? this['driver_memory'] : '512M';
            var driver_memory = parseMemory(driver_memory_str);
            var executor_memory_str = this['executor_memory'] ? this['executor_memory'] : '1024M';
            var executor_memory = parseMemory(executor_memory_str);
            var num_executors = this['num_executors'] ? this['num_executors'] : 1;
            var memoryOverhead;
            if (this['spark_other_args'] && this['spark_other_args'].indexOf('spark.yarn.executor.memoryOverhead=') !== -1) {
                var pos = this['spark_other_args'].indexOf('spark.yarn.executor.memoryOverhead=');
                var memoryOverhead_str = this['spark_other_args'].substring(pos + 'spark.yarn.executor.memoryOverhead='.length).split(' ')[0];
                if (memoryOverhead_str) {
                    memoryOverhead = parseMemory(memoryOverhead_str);
                }
            }
            if (memoryOverhead) {
                return (num_executors * (executor_memory + memoryOverhead) + (driver_memory + memoryOverhead)) + 'M';
            } else {
                return (num_executors * (executor_memory + Math.max(executor_memory * 0.1, 384)) + (driver_memory + Math.max(driver_memory * 0.1, 384))) + 'M';
            }
        },
        '$cal_total_cores': function () {
            var driver_cores = this['driver_cores'] ? parseInt(this['driver_cores']) : 1;
            var num_executors = this['num_executors'] ? parseInt(this['num_executors']) : 1;
            var executor_cores = this['executor_cores'] ? parseInt(this['executor_cores']) : 1;
            return num_executors * executor_cores + driver_cores;
        }
    };
    $scope.Flink = {
        'createDefault': function () {
            return {
                '$command': $scope.flinkVersionList[0]['command'],
                'm': 'yarn-cluster',
                'yjm': '512',
                'ytm': '1024',
                'yn': '1',
                'ys': '1',
                'flink_other_args': '-d',
                '$total_memory': this['$cal_total_memory'],
                '$total_cores': this['$cal_total_cores']
            };
        },
        'createEmpty': function () {
            return {
                '$total_memory': this['$cal_total_memory'],
                '$total_cores': this['$cal_total_cores']
            }
        },
        '$cal_total_memory': function () {
            var yjm_str = this['yjm'] ? this['yjm'] : '512M';
            var yjm = parseMemory(yjm_str);
            var ytm_str = this['ytm'] ? this['ytm'] : '1024M';
            var ytm = parseMemory(ytm_str);
            var yn = this['yn'] ? this['yn'] : 1;
            return (yn * ytm + yjm) + 'M';
        },
        '$cal_total_cores': function () {
            var yn = this['yn'] ? parseInt(this['yn']) : 1;
            var ys = this['ys'] ? parseInt(this['ys']) : 1;
            return yn * ys + 1;
        }
    };

    $scope.onScriptObjChange = function() {
        var script = getScript();
        if (script.editMode_ === 'visual') {
            // 清理上传状态
            $('#dropzone_filetable').empty();
            $a_visual.tab('show');
            $timeout(function () {
                $('.selectpicker').selectpicker('render');
            }, 50);
            $timeout(function () {
                $('[data-toggle="tooltip"]').tooltip();
            }, 50);
        } else {
            $a_text.tab('show');
            $timeout(function () {
                $('.selectpicker').selectpicker('render');
            }, 50);
        }
        if (!$('#advancedDropzone').hasClass('dz-clickable')) {
            $timeout(function() {
                initUpload();
            }, 50);
        }
    };

    $scope.onTypeChange = function () {
        var script = getScript();
        script.content = '';
        if (script.type !== 'shell') {
            if (script.type === 'sparkstream' || script.type === 'flinkstream') {
                script.nodeBlackListType_ = 'stream';
                script.allocateBalancerType_ = 'total';
            } else {
                script.nodeBlackListType_ = 'batch';
                script.allocateBalancerType_ = 'available';
            }
            $scope.onNodeBlackListTypeChange();
            $scope.onAllocateBalancerTypeChange();
            // 解析 content
            $scope.parseVisual(script);
            $timeout(function () {
                $('[data-toggle="tooltip"]').tooltip();
            }, 50);
        } else {
            $a_text.tab('show');
            $timeout(function () {
                $('.selectpicker').selectpicker('render');
            }, 50);
        }
        if (!$('#advancedDropzone').hasClass('dz-clickable')) {
            $timeout(function() {
                initUpload();
            }, 50);
        }
    };

    $scope.onSparkVersionChange = function() {
        var script = getScript();
        var command = script.spark.$command;
        var content = script.content;
        if (content) {
            if (content.indexOf(' ') !== -1) {
                content = command + content.substring(content.indexOf(' '));
            } else {
                content = command + ' ';
            }
        } else {
            content = command + ' ';
        }
        script.content = content;
    };

    $scope.onFlinkVersionChange = function() {
        var script = getScript();
        var command = script.flink.$command;
        var content = script.content;
        if (content) {
            if (content.indexOf(' run ') !== -1) {
                content = command + content.substring(content.indexOf(' run '));
            } else {
                content = command + ' run ';
            }
        } else {
            content = command + ' run ';
        }
        script.content = content;
    };

    $scope.onClusterIdChange = function () {
        var script = getScript();
        var uid = script.createBy || $scope.user.id;
        var clusterId = script.clusterId || '';
        var clusterUser = $scope.clusterUserMap[uid + '_' + clusterId];
        var tmp;
        if (clusterUser) {
            var queueArr = clusterUser.queue.split(',');
            $scope.queueList = queueArr;
            script.queue_ = queueArr[0];
            script.spark.queue = queueArr[0];
            script.flink.yqu = queueArr[0];
            if (clusterUser.user) {
                script.spark.proxy_user = clusterUser.user;
                tmp = script.flink.flink_other_args;
                if (tmp) {
                    if (tmp.indexOf('-yD ypu=') !== -1) {
                        tmp = tmp.replace(/-yD ypu=[\w-.,]+/g, '-yD ypu=' + clusterUser.user);
                    } else {
                        tmp += ' -yD ypu=' + clusterUser.user;
                    }
                } else {
                    tmp = '-yD ypu=' + clusterUser.user;
                }
                script.flink.flink_other_args = tmp;
            }
        } else {
            $scope.queueList = [];
            script.queue_ = '';
            script.spark.queue = '';
            script.flink.yqu = '';
            script.spark.proxy_user = '';
            tmp = script.flink.flink_other_args;
            if (tmp && tmp.indexOf('-yD ypu=') !== -1) {
                tmp = tmp.replace(/\s*-yD ypu=[\w-.,]+/g, '').resetBlank();
                if (tmp.length >= 1 && tmp.substring(0, 1) === ' ') {
                    tmp = tmp.substring(1);
                }
                script.flink.flink_other_args = tmp;
            }
        }
        // 刷新队列
        $timeout(function () {
            $('.selectpicker').selectpicker('refresh');
        }, 50);
        $scope.onNodeBlackListTypeChange();
        $scope.parseVisual(script);
    };

    $scope.onQueueChange = function () {
        var script = getScript();
        script.spark.queue = script.queue_ || '';
        script.flink.yqu = script.queue_ || '';
        $scope.parseVisual(script);
    };

    $scope.onJarPathVerify = function () {
        var script = getScript();
        var jarPath_ = script.jarPath_;
        if (jarPath_) {
            if (!jarPath_.endsWith('.jar') && !jarPath_.endsWith('.py')) {
                swal('程序包检验错误', '文件格式错误', 'error');
                return;
            }
            $scope.parseVisual(script);
        }
    };

    $scope.onNodeBlackListTypeChange = function() {
        var script = getScript();
        var type = script.nodeBlackListType_;
        var dealBlackListType = function(type, target) {
            if (!target) {
                target = '';
            }
            var cluster = $scope.clusterMap[script.clusterId];
            var nodeList;
            if (type && cluster) {
                if (type === 'stream'){
                    if (cluster['streamBlackNodeList']) {
                        nodeList = cluster['streamBlackNodeList'];
                    } else {
                        swal({
                            position: 'center',
                            type: 'warning',
                            title: '当前集群暂无屏蔽策略对应黑名单',
                            showConfirmButton: false,
                            timer: 2000,
                            toast: true
                        });
                    }
                } else {
                    if (cluster['batchBlackNodeList']) {
                        nodeList = cluster['batchBlackNodeList'];
                    } else {
                        swal({
                            position: 'center',
                            type: 'warning',
                            title: '当前集群暂无屏蔽策略对应黑名单',
                            showConfirmButton: false,
                            timer: 2000,
                            toast: true
                        });
                    }
                }
            }
            if (nodeList) {
                if (target.indexOf('--node.black.list=') !== -1) {
                    target = target.replace(/--node.black.list=[\w-.,]+/g, '--node.black.list=' + nodeList).resetBlank();
                } else {
                    if (target) {
                        target += ' --node.black.list=' + nodeList;
                    }  else {
                        target += '--node.black.list=' + nodeList;
                    }
                }
            } else {
                if (target.indexOf('--node.black.list=') !== -1) {
                    target = target.replace(/\s*--node.black.list=[\w-.,]+/g, '').resetBlank();
                    if (target.length >= 1 && target.substring(0, 1) === ' ') {
                        target = target.substring(1);
                    }
                }
            }
            return target;
        };
        script.spark.args = dealBlackListType(type, script.spark.args);
        script.flink.args = dealBlackListType(type, script.flink.args);
        script.content = dealBlackListType(type, script.content);
    };

    $scope.onAllocateBalancerTypeChange = function() {
        var script = getScript();
        var type = script.allocateBalancerType_;
        var dealAllocateBalancerType = function(type, target) {
            if (!target) {
                target = '';
            }
            if (type) {
                if (target.indexOf('--allocate.balancer.type=') !== -1) {
                    target = target.replace(/--allocate.balancer.type=[\w-.,]+/g, '--allocate.balancer.type=' + type).resetBlank();
                } else {
                    if (target) {
                        target += ' --allocate.balancer.type=' + type;
                    }  else {
                        target += '--allocate.balancer.type=' + type;
                    }
                }
            } else {
                if (target.indexOf('--allocate.balancer.type=') !== -1) {
                    target = target.replace(/\s*--allocate.balancer.type=[\w-.,]+/g, '').resetBlank();
                    if (target.length >= 1 && target.substring(0, 1) === ' ') {
                        target = target.substring(1);
                    }
                }
            }
            return target;
        };
        script.spark.args = dealAllocateBalancerType(type, script.spark.args);
        script.flink.args = dealAllocateBalancerType(type, script.flink.args);
        script.content = dealAllocateBalancerType(type, script.content);
    };

    $scope.parseText = function (script) {
        if (script.type === 'sparkbatch' || script.type === 'sparkstream') {
            $scope.parseSparkText(script);
        } else if (script.type === 'flinkbatch' || script.type === 'flinkstream') {
            $scope.parseFlinkText(script);
        }
        $timeout(function () {
            $('.selectpicker').selectpicker('render');
        }, 50);
    };
    $scope.parseVisual = function (script) {
        if (script.type === 'sparkbatch' || script.type === 'sparkstream') {
            $scope.parseSparkVisual(script);
        } else if (script.type === 'flinkbatch' || script.type === 'flinkstream') {
            $scope.parseFlinkVisual(script);
        }
        $timeout(function () {
            $('.selectpicker').selectpicker('render');
        }, 50);
    };
    $scope.parseSparkText = function (script) {
        var tokens = script.content ? script.content.resetBlank().split(' ') : [];
        var spark;
        var jarIndex;
        var key;
        //获取jar包索引位
        tokens.forEach(function(value, index) {
            if (value.indexOf('.jar') !== -1 || value.indexOf('.py') !== -1) {
                if (tokens[index - 1] !== '--jars' && tokens[index - 1] !== '-j' && tokens[index - 1] !== '--jar') {
                    if (jarIndex === undefined) {
                        jarIndex = index;
                    }
                }
            }
        });
        if (jarIndex === undefined) {
            script.jarPath_ = '';
        }
        if (tokens.length) {
            spark = $scope.Spark.createEmpty();
            for (var i = 0; i < tokens.length; i ++) {
                var token = tokens[i].trim();
                if (!token) {
                    continue;
                }
                if ($scope.sparkVersionMap[token]) {
                    spark['$command'] = token;
                    continue;
                }
                if (jarIndex !== undefined && i === jarIndex) {
                    if (!script.jarPath_) {
                        script.jarPath_ = token;
                    } else {
                        script.content = script.content.replace(token, script.jarPath_.trim());
                    }
                    continue;
                }
                if (token.startsWith('--conf')) {
                    if (i >= tokens.length - 1) {
                        return;
                    }
                    token = tokens[++i];
                    if (!token) {
                        return;
                    }
                    var tokenArr = token.split('=');
                    key = sparkArgKey[tokenArr[0]];
                    if (key) {
                        spark[key] = tokenArr[1];
                    } else {
                        if (jarIndex !== undefined && i > jarIndex) {
                            if (spark.args) {
                                spark.args += ' --conf ' + token;
                            } else {
                                spark.args = '--conf ' + token;
                            }
                        } else {
                            if (spark.spark_other_args) {
                                spark.spark_other_args += ' --conf ' + token;
                            } else {
                                spark.spark_other_args = '--conf ' + token;
                            }
                        }
                    }
                } else {
                    if ($scope.caseResource(token, spark, script)) {
                        continue;
                    }
                    key = sparkArgKey[token.substring('--'.length)];
                    if (key) {
                        spark[key] = tokens[++i];
                    } else {
                        if (jarIndex !== undefined && i > jarIndex) {
                            if (spark.args) {
                                spark.args += ' ' + token;
                            } else {
                                spark.args = token;
                            }
                        } else {
                            if (spark.spark_other_args) {
                                spark.spark_other_args += ' ' + token;
                            } else {
                                spark.spark_other_args = token;
                            }
                        }
                    }
                }
            }
        } else {
            spark = $scope.Spark.createDefault();
        }
        script.spark = spark;
    };
    $scope.parseSparkVisual = function (script) {
        var spark = script.spark;
        var content = script.content || '';
        content = content.resetBlank();
        if (content) {
            if (!content.startsWith(spark.$command)) {
                content = spark.$command + ' ' + content;
            }
        } else {
            content = spark.$command + ' ';
        }
        content = content.substring(0, content.indexOf(' '));
        for (var key in spark){
            var keyTmp = key;
            if (keyTmp !== 'args' && keyTmp !== 'spark_other_args' && keyTmp.indexOf('$') === -1) {
                var value = spark[keyTmp];
                keyTmp = keyTmp.replace(/_/g, '-');
                if (value) {
                    value = value.toString().trim();
                    content += ' --' + keyTmp + ' ' + value;
                }
            }
        }
        if (spark['spark_other_args']) {
            content += ' ' + spark['spark_other_args'].trim();
        }
        if (script.jarPath_) {
            content += ' ' + script.jarPath_.trim();
        }
        if (spark['args']) {
            content += ' ' + spark['args'].trim();
        }
        script.content = content;
    };
    $scope.parseFlinkText = function (script) {
        var tokens = script.content ? script.content.resetBlank().split(' ') : [];
        var flink;
        var jarIndex;
        var key;
        //获取jar包索引位
        tokens.forEach(function (value, index) {
            if (value.indexOf('.jar') !== -1 || value.indexOf('.py') !== -1) {
                if (tokens[index - 1] !== '--jars' && tokens[index - 1] !== '-j' && tokens[index - 1] !== '--jar') {
                    if (jarIndex === undefined) {
                        jarIndex = index;
                    }
                }
            }
        });
        if (jarIndex === undefined) {
            script.jarPath_ = '';
        }
        if (tokens.length) {
            flink = $scope.Flink.createEmpty();
            for (var i = 0; i < tokens.length; i ++) {
                var token = tokens[i].trim();
                if (!token) {
                    continue;
                }
                if ($scope.flinkVersionMap[token]) {
                    flink['$command'] = token;
                    continue;
                }
                if (token === 'run') {
                    continue;
                }
                if (jarIndex !== undefined && i === jarIndex) {
                    if (!script.jarPath_) {
                        script.jarPath_ = token;
                    } else {
                        script.content = script.content.replace(token, script.jarPath_.trim());
                    }
                    continue;
                }
                if ($scope.caseResource(token, flink, script)) {
                    continue;
                }
                key = flinkArgKey[token.substring('-'.length)];
                if (key) {
                    flink[key] = tokens[++i];
                } else {
                    if (jarIndex !== undefined && i > jarIndex) {
                        if (flink.args) {
                            flink.args += ' ' + token;
                        } else {
                            flink.args = token;
                        }
                    } else {
                        if (flink.flink_other_args) {
                            flink.flink_other_args += ' ' + token;
                        } else {
                            flink.flink_other_args = token;
                        }
                    }
                }
            }
        } else {
            flink = $scope.Flink.createDefault();
        }
        script.flink = flink;
    };
    $scope.parseFlinkVisual = function (script) {
        var flink = script.flink;
        var content = script.content ? script.content : '';
        content = content.resetBlank();
        if (content !== '') {
            if (!content.startsWith(flink.$command)) {
                content = flink.$command + ' run ' + content;
            }
        } else {
            content = flink.$command + ' run ';
        }
        content = content.substring(0, content.indexOf('run') + 'run'.length);
        for (var key in flink){
            var keyTmp = key;
            if (keyTmp !== 'args' && keyTmp !== 'flink_other_args' && keyTmp.indexOf('$') === -1) {
                var value = flink[keyTmp];
                if (keyTmp.startsWith('flink')) {
                    keyTmp = keyTmp.replace(/_/g, ".");
                }
                keyTmp = '-' + keyTmp;
                if (value) {
                    value = value.toString().trim();
                    content += ' ' + keyTmp + ' ' + value;
                }
            }
        }
        if (flink['flink_other_args']) {
            content += ' ' + flink['flink_other_args'].trim();
        }
        if (script.jarPath_) {
            content += ' ' + script.jarPath_.trim();
        }
        if (flink['args']) {
            content += ' ' + flink['args'].trim();
        }
        script.content = content;
    };
    $scope.caseResource = function (token, target, script) {
        //处理屏蔽节点
        if (token.indexOf('--node.black.list=') !== -1) {
            if (script.clusterId) {
                var cluster = $scope.clusterMap[script.clusterId];
                if (token.split('=')[1] === cluster['streamBlackNodeList']) {
                    script.nodeBlackListType_ = 'stream';
                } else if (token.split('=')[1] === cluster['batchBlackNodeList']) {
                    script.nodeBlackListType_ = 'batch';
                } else {
                    script.nodeBlackListType_ = '';
                }
                if (target.args) {
                    target.args += ' ' + token;
                } else {
                    target.args = token;
                }
            }
            return true;
        }
        //处理容器分配策略
        if (token.indexOf('--allocate.balancer.type=') !== -1) {
            if (token.split('=')[1] === 'total' || token.split('=')[1] === 'available') {
                script.allocateBalancerType_ = token.split('=')[1];
            } else {
                script.allocateBalancerType_ = '';
            }
            if (target.args) {
                target.args += ' ' + token;
            } else {
                target.args = token;
            }
            return true;
        }
        return false;
    };

    $timeout(function() {
        initUpload();
    }, 50);

}
