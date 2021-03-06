$(function () {
    $("#jqGrid").Grid({
        url: '../dishes/list',
        colModel: [
			{label: 'id', name: 'id', index: 'id', key: true, hidden: true},
			{label: '菜品名字', name: 'dishesName', index: 'dishes_name', width: 80},
            {label: '菜品类型', name: 'categoryname', index: 'category_name', width: 80},
			{label: '菜品描述',name:'dishesDescribe',index:'dishes_describe',width: 80},
			{label: '菜品封面图片', name: 'dishesCoverPic', index: 'dishes_cover_pic', width: 80,
                formatter: function (value) {
                    return transImg(value);}},
			{label: '菜品卡路里', name: 'dishesCalories', index: 'dishes_calories', width: 80},
            // {  label:'所需食材',name:'check', width: 80,index:'check',sortable:false, formatter:function(value,col,row){
            //     return  '<button  style="width: 40px;line-height: 30px" onclick="vm.lookDetail('+  row.id  +')">查看</button>'
            //     }
            // },
        ]
    });
    vm.getcategory();
});

let vm = new Vue({
	el: '#rrapp',
	data: {
        showList: true,
        title: null,
		dishes: {},
        listtype:{},
		ruleValidate: {
			name: [
				{required: true, message: '名称不能为空', trigger: 'blur'}
			]
		},
		q: {
		    name: ''
		},
        category:[]
	},
	methods: {
		query: function () {
			vm.reload();
		},
		add: function () {
			vm.showList = false;
			vm.title = "新增";
			vm.dishes = {};
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
            let url = vm.dishes.id == null ? "../dishes/save" : "../dishes/update";
            Ajax.request({
			    url: url,
                params: JSON.stringify(vm.dishes),
                type: "POST",
			    contentType: "application/json",
                successCallback: function (r) {
                    alert('操作成功', function (index) {
                        vm.reload();
                    });
                }
			});
		},
        foodint: function () {
            var id = getSelectedRow("#jqGrid");
            if (id == null) {
                return;
            }
            openWindow({
                type: 2,
                title: '所需食材',
                content: '../shop/foodingredients.html?dishesId=' + id
            })
        },
        dishestep: function () {
            var id = getSelectedRow("#jqGrid");
            if (id == null) {
                return;
            }
            openWindow({
                type: 2,
                title: '所需食材',
                content: '../shop/dishessteps.html?dishesId=' + id
            })
        },
		del: function (event) {
            let ids = getSelectedRows("#jqGrid");
			if (ids == null){
				return;
			}

			confirm('确定要删除选中的记录？', function () {
                Ajax.request({
				    url: "../dishes/delete",
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
                url: "../dishes/info/"+id,
                async: true,
                successCallback: function (r) {
                    vm.dishes = r.dishes;
                }
            });
		},
		reload: function (event) {
			vm.showList = true;
            let page = $("#jqGrid").jqGrid('getGridParam', 'page');
			$("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.q.dishesName},
                page: page
            }).trigger("reloadGrid");
            vm.handleReset('formValidate');
		},
        reloadSearch: function() {
            vm.q = {
                dishesName: ''
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
        handleSuccessdishesCoverPic: function (res, file) {
            vm.dishes.dishesCoverPic = file.response.url;
        },
        handleMaxSize: function (file) {
            this.$Notice.warning({
                title: '超出文件大小限制',
                desc: '文件 ' + file.name + ' 太大，不能超过 20m。'
            });
        },
        eyeImagedishesCoverPic: function () {
            var url =vm.dishes.dishesCoverPic;
            eyeImage(url);
        }
	}
});