$(function () {
    $("#jqGrid").Grid({
        url: '../foodmaterial/list',
        colModel: [
			{label: 'id', name: 'id', index: 'id', key: true, hidden: true},
			{label: '食材名', name: 'foodMaterialName', index: 'food_material_name', width: 80},
            {label: '食材类型', name: 'typeName', index: 'food_type_id', width: 80},
			{label: '食材图片', name: 'foodMaterialPic', index: 'food_material_pic', width: 80,
                formatter: function (value) {
                    return transImg(value);}},
			{label: '食材描述', name: 'foodMaterialDescribe', index: 'food_material_describe', width: 80},
			{label: '食材卡路里', name: 'foodMaterialCalories', index: 'food_material_calories', width: 80},
            {label: '食材单位', name: 'foodUnit', index: 'food_unit', width: 80},
			]
    });
});

let vm = new Vue({
	el: '#rrapp',
	data: {
        showList: true,
        title: null,
		foodMaterial: {},
		ruleValidate: {
            name: [
				{required: true, message: '名称不能为空', trigger: 'blur'}
			]
		},
		q: {
		    name: ''
		},
        foodType:[],
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function () {
			vm.showList = false;
			vm.title = "新增";
			vm.foodMaterial = {};
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
            let url = vm.foodMaterial.id == null ? "../foodmaterial/save" : "../foodmaterial/update";
            Ajax.request({
			    url: url,
                params: JSON.stringify(vm.foodMaterial),
                type: "POST",
			    contentType: "application/json",
                successCallback: function (r) {
                    alert('操作成功', function (index) {
                        vm.reload();
                    });
                }
			});
		},
        foodnutr: function () {
            var id = getSelectedRow("#jqGrid");
            if (id == null) {
                return;
            }
            openWindow({
                type: 2,
                title: '营养元素',
                content: '../shop/foodnutrientelements.html?foodMaterialId=' + id
            })
        },
		del: function (event) {
            let ids = getSelectedRows("#jqGrid");
			if (ids == null){
				return;
			}

			confirm('确定要删除选中的记录？', function () {
                Ajax.request({
				    url: "../foodmaterial/delete",
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
                url: "../foodmaterial/info/"+id,
                async: true,
                successCallback: function (r) {
                    vm.foodMaterial = r.foodMaterial;
                }
            });
		},
        getType :function(){
		    Ajax.request({
                url:"../foodtype/list",
                async: true,
                successCallback:function(r){
                    vm.foodType = r.list;
                }
            });
        },
		reload: function (event) {
			vm.showList = true;
            let page = $("#jqGrid").jqGrid('getGridParam', 'page');
			$("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.q.foodMaterialName},
                page: page
            }).trigger("reloadGrid");
            vm.handleReset('formValidate');
		},
        reloadSearch: function() {
            vm.q = {
                foodMaterialName: ''
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
        },
        handleFormatError: function (file) {
            this.$Notice.warning({
                title: '文件格式不正确',
                desc: '文件 ' + file.name + ' 格式不正确，请上传 jpg 或 png 格式的图片。'
            });
        },
        handleSuccessfoodMaterialPic: function (res, file) {
            vm.foodMaterial.foodMaterialPic = file.response.url;
        },
        handleMaxSize: function (file) {
            this.$Notice.warning({
                title: '超出文件大小限制',
                desc: '文件 ' + file.name + ' 太大，不能超过 20m。'
            });
        },
        eyeImagefoodMaterialPic: function () {
            var url =vm.foodMaterial.foodMaterialPic;
            eyeImage(url);
        }
	}
});