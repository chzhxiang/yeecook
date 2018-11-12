$(function () {
    $("#jqGrid").Grid({
        url: '../activationgeneratorlog/list',
        colModel: [
			{label: 'id', name: 'id', index: 'id', key: true, hidden: true},
			{label: '关联激活码生成器id', name: 'activationGeneratorId', index: 'activation_generator_id', width: 80},
			{label: '生成激活码数量', name: 'count', index: 'count', width: 80},
			{label: '生成激活码秘钥', name: 'password', index: 'password', width: 80},
			{label: '分组id', name: 'groupId', index: 'group_id', width: 80},
			{label: '激活码生成长度', name: 'length', index: 'length', width: 80},
			{label: '操作用户', name: 'userId', index: 'user_id', width: 80},
			{label: '操作时间', name: 'addTime', index: 'add_time', width: 80},
			{label: '操作ip', name: 'ip', index: 'ip', width: 80},
			{label: '关联服务id', name: 'serveInfoId', index: 'serve_info_id', width: 80}]
    });
});

let vm = new Vue({
	el: '#rrapp',
	data: {
        showList: true,
        title: null,
		activationGeneratorLog: {},
		ruleValidate: {
			name: [
				{required: true, message: '名称不能为空', trigger: 'blur'}
			]
		},
		q: {
		    name: ''
		}
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function () {
			vm.showList = false;
			vm.title = "新增";
			vm.activationGeneratorLog = {};
		},
		update: function (event) {
            let id = getSelectedRow("#jqGrid");
			if (id == null) {
				return;
			}
			vm.showList = false;
            vm.title = "修改";

            vm.getInfo(id)
		},
		saveOrUpdate: function (event) {
            let url = vm.activationGeneratorLog.id == null ? "../activationgeneratorlog/save" : "../activationgeneratorlog/update";
            Ajax.request({
			    url: url,
                params: JSON.stringify(vm.activationGeneratorLog),
                type: "POST",
			    contentType: "application/json",
                successCallback: function (r) {
                    alert('操作成功', function (index) {
                        vm.reload();
                    });
                }
			});
		},
		del: function (event) {
            let ids = getSelectedRows("#jqGrid");
			if (ids == null){
				return;
			}

			confirm('确定要删除选中的记录？', function () {
                Ajax.request({
				    url: "../activationgeneratorlog/delete",
                    params: JSON.stringify(ids),
                    type: "POST",
				    contentType: "application/json",
                    successCallback: function () {
                        alert('操作成功', function (index) {
                            vm.reload();
                        });
					}
				});
			});
		},
		getInfo: function(id){
            Ajax.request({
                url: "../activationgeneratorlog/info/"+id,
                async: true,
                successCallback: function (r) {
                    vm.activationGeneratorLog = r.activationGeneratorLog;
                }
            });
		},
		reload: function (event) {
			vm.showList = true;
            let page = $("#jqGrid").jqGrid('getGridParam', 'page');
			$("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.q.name},
                page: page
            }).trigger("reloadGrid");
            vm.handleReset('formValidate');
		},
        reloadSearch: function() {
            vm.q = {
                name: ''
            }
            vm.reload();
        },
        handleSubmit: function (name) {
            handleSubmitValidate(this, name, function () {
                vm.saveOrUpdate()
            });
        },
        handleReset: function (name) {
            handleResetForm(this, name);
        }
	}
});