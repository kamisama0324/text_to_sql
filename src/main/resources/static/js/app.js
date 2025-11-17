// Vue.js 应用主逻辑
const { createApp, ref, reactive, computed, onMounted, onUnmounted, nextTick } = Vue;
const { ElMessage, ElMessageBox, ElNotification } = ElementPlus;

// API 基础配置
const API_BASE = '/api/mcp';
const DATASOURCE_API = '/api/datasources';

// 创建 Vue 应用
const app = createApp({
    setup() {
        // 响应式数据
        const connectionStatus = ref(false);
        const checking = ref(false);
        const loadingSchema = ref(false);
        const converting = ref(false);
        const executing = ref(false);
        
        // 数据源管理相关数据
        const showDatasourceModal = ref(false);
        const editingDatasource = ref(false);
        const loadingDataSources = ref(false);
        const savingDatasource = ref(false);
        const selectedDataSourceId = ref(null);
        const availableDataSources = ref([]);
        const currentUserDataSource = ref(null);
        const showDatasourceDropdown = ref(false);
        const currentDatasource = reactive({
            id: null,
            name: '',
            type: 'mysql',
            host: 'localhost',
            port: 3306,
            database: '',
            username: '',
            password: '',
            minimumIdle: 5,
            maximumPoolSize: 10,
            connectionTimeout: 30000,
            sslEnabled: false,
            description: '',
            active: true
        });
        
        const userQuery = ref('');
        const currentDatabase = ref('');
        const schema = reactive({
            tables: [],
            totalColumns: 0
        });
        
        const activeTableNames = ref([]);
        const showResults = ref(false);
        const activeTab = ref('sql');
        
        const generatedSql = ref('');
        const sqlExplanation = ref('');
        const queryResults = ref(null);
        const rawResponse = ref(null);
        
        // 反馈相关数据
        const feedbackSubmitted = ref(false);
        const submittingFeedback = ref(false);
        const showFeedbackModal = ref(false);
        const feedbackType = ref(''); // 'correct', 'incorrect', 'correction'
        const correctedSql = ref('');
        const feedbackDescription = ref('');

        // 示例查询
        const exampleQueries = ref([
            '查询所有用户信息',
            '统计订单总数',
            '查找今天注册的用户',
            '按月份统计销售额',
            '查询最受欢迎的商品',
            '找出活跃用户数量',
            '统计各类别商品数量',
            '查询本月新增客户'
        ]);

        // 方法定义
        const showError = (message, title = '错误') => {
            ElMessage({
                type: 'error',
                message: message,
                duration: 5000,
                showClose: true,
                offset: 100 // 增大偏移量，确保显示在顶部可见区域
            });
            console.error(title + ':', message);
        };
        
        // 初始化时加载用户的数据源
        onMounted(() => {
            loadDataSources();
        });

        const showSuccess = (message) => {
            ElMessage({
                type: 'success',
                message: message,
                duration: 3000,
                offset: 100 // 增大偏移量，确保显示在顶部可见区域
            });
        };

        const showInfo = (message) => {
            ElMessage({
                type: 'info',
                message: message,
                duration: 3000,
                offset: 100 // 增大偏移量，确保显示在顶部可见区域
            });
        };

        // 数据源管理方法
        const loadDataSources = async () => {
            try {
                loadingDataSources.value = true;
                console.log('开始加载数据源...');
                const response = await apiRequest(DATASOURCE_API + '?t=' + Date.now()); // 添加时间戳防止缓存
                console.log('数据源API响应:', response);
                const allDataSources = response.data || response || [];
                console.log('解析到的数据源数量:', allDataSources.length);

                // 限制每个用户只能有一个数据源，直接取第一个
                availableDataSources.value = allDataSources;
                currentUserDataSource.value = allDataSources[0] || null;

                // 如果有数据源，自动选中第一个
                if (currentUserDataSource.value) {
                    selectedDataSourceId.value = currentUserDataSource.value.id;
                    onDataSourceChange();
                } else {
                    selectedDataSourceId.value = null;
                }
            } catch (error) {
                showError('加载数据源失败: ' + error.message);
                console.error('Failed to load datasources:', error);
            } finally {
                loadingDataSources.value = false;
            }
        };
        
        const toggleDatasourceDropdown = () => {
            showDatasourceDropdown.value = !showDatasourceDropdown.value;
            // 点击外部关闭悬浮框
            if (showDatasourceDropdown.value) {
                setTimeout(() => {
                    document.addEventListener('click', closeDropdownOnClickOutside);
                }, 10);
            } else {
                document.removeEventListener('click', closeDropdownOnClickOutside);
            }
        };
        
        const closeDropdownOnClickOutside = (event) => {
            const dropdownWrapper = document.querySelector('.datasource-dropdown-wrapper');
            if (dropdownWrapper && !dropdownWrapper.contains(event.target)) {
                showDatasourceDropdown.value = false;
                document.removeEventListener('click', closeDropdownOnClickOutside);
            }
        };
        
        // 在组件卸载时清理事件监听器
        onUnmounted(() => {
            document.removeEventListener('click', closeDropdownOnClickOutside);
        });
        
        const editCurrentDatasource = () => {
            if (currentUserDataSource.value) {
                showDatasourceDropdown.value = false;
                Object.assign(currentDatasource, currentUserDataSource.value);
                editingDatasource.value = true;
                showDatasourceModal.value = true;
            }
        };
        
        const testCurrentUserConnection = async () => {
            if (currentUserDataSource.value) {
                showDatasourceDropdown.value = false;
                try {
                    // 创建一个包含所有字段的对象，并确保password和encryptedPassword字段都被正确发送
                    const passwordValue = currentUserDataSource.value.password || '';
                    const datasourceData = {
                        ...currentUserDataSource.value,
                        password: passwordValue,
                        encryptedPassword: passwordValue // 同时发送到encryptedPassword字段以满足后端验证
                    };
                    
                    const response = await apiRequest(DATASOURCE_API + '/test', {
                        method: 'POST',
                        body: JSON.stringify(datasourceData),
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });
                    
                    if (response && response.success) {
                        showSuccess('连接测试成功！');
                    } else {
                        showError(response?.message || '连接测试失败');
                    }
                } catch (error) {
                    showError('连接测试失败: ' + error.message);
                }
            }
        };
        
        const deleteCurrentDatasource = async () => {
            if (currentUserDataSource.value) {
                try {
                    await ElMessageBox.confirm(
                        `确定要删除数据源 "${currentUserDataSource.value.name}" 吗？删除后将无法恢复。`,
                        '确认删除',
                        {
                            confirmButtonText: '确定',
                            cancelButtonText: '取消',
                            type: 'warning'
                        }
                    );
                    
                    showDatasourceDropdown.value = false;
                    const response = await apiRequest(`${DATASOURCE_API}/${currentUserDataSource.value.id}`, {
                        method: 'DELETE'
                    });
                    
                    if (response.success || response) {
                        showSuccess('数据源删除成功');
                        currentUserDataSource.value = null;
                        selectedDataSourceId.value = null;
                        schema.tables = [];
                        currentDatabase.value = '';
                    } else {
                        showError('数据源删除失败');
                    }
                } catch (error) {
                    if (error !== 'cancel') {
                        showError('删除数据源失败: ' + error.message);
                    }
                }
            }
        };
        
        const getDatabaseTypeName = (type) => {
            const typeNames = {
                'mysql': 'MySQL',
                'postgresql': 'PostgreSQL',
                'oracle': 'Oracle',
                'sqlserver': 'SQL Server',
                'h2': 'H2 Database'
            };
            return typeNames[type] || type;
        };

        const openDataSourceManager = () => {
            // 不再使用这个方法，而是通过悬浮框管理数据源
            showDatasourceModal.value = true;
            editingDatasource.value = false;
            loadDataSources();
        };

        const closeDatasourceManager = () => {
            showDatasourceModal.value = false;
            editingDatasource.value = false;
            resetDatasourceForm();
        };

        const resetDatasourceForm = () => {
            currentDatasource.id = null;
            currentDatasource.name = '';
            currentDatasource.type = 'mysql';
            currentDatasource.host = 'localhost';
            currentDatasource.port = 3306;
            currentDatasource.database = '';
            currentDatasource.username = '';
            currentDatasource.password = '';
            currentDatasource.minimumIdle = 5;
            currentDatasource.maximumPoolSize = 10;
            currentDatasource.connectionTimeout = 30000;
            currentDatasource.sslEnabled = false;
            currentDatasource.description = '';
            currentDatasource.active = true;
        };

        const startCreateDatasource = () => {
            showDatasourceDropdown.value = false;  // 关闭下拉菜单
            editingDatasource.value = true;
            resetDatasourceForm();
            
            // 根据数据库类型设置默认端口
            const portMap = {
                'mysql': 3306,
                'postgresql': 5432,
                'oracle': 1521,
                'sqlserver': 1433,
                'h2': 9092
            };
            currentDatasource.port = portMap[currentDatasource.type];
            
            // 显示数据源编辑弹窗
            showDatasourceModal.value = true;
        };

        const editDataSource = (id) => {
            const ds = availableDataSources.value.find(item => item.id === id);
            if (ds) {
                Object.assign(currentDatasource, ds);
                editingDatasource.value = true;
            }
        };

        const cancelEdit = () => {
            editingDatasource.value = false;
            resetDatasourceForm();
        };

        const testCurrentConnection = async () => {
            try {
                savingDatasource.value = true;
                // 创建一个包含所有字段的对象，并确保password和encryptedPassword字段都被正确发送
                const passwordValue = currentDatasource.password || '';
                const datasourceData = {
                    ...currentDatasource,
                    password: passwordValue,
                    encryptedPassword: passwordValue // 同时发送到encryptedPassword字段以满足后端验证
                };
                
                const response = await apiRequest(DATASOURCE_API + '/test', {
                    method: 'POST',
                    body: JSON.stringify(datasourceData),
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response && response.success) {
                    showSuccess('连接测试成功！');
                } else {
                    showError(response?.message || '连接测试失败');
                }
            } catch (error) {
                showError('连接测试失败: ' + error.message);
            } finally {
                savingDatasource.value = false;
            }
        };

        const testDataSourceConnection = async (id) => {
            try {
                const ds = availableDataSources.value.find(item => item.id === id);
                if (!ds) {
                    showError('数据源不存在');
                    return;
                }
                
                // 创建一个包含所有字段的对象，并确保password和encryptedPassword字段都被正确发送
                const passwordValue = ds.password || '';
                const datasourceData = {
                    ...ds,
                    password: passwordValue,
                    encryptedPassword: passwordValue // 同时发送到encryptedPassword字段以满足后端验证
                };
                
                const response = await apiRequest(DATASOURCE_API + '/test', {
                    method: 'POST',
                    body: JSON.stringify(datasourceData),
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response && response.success) {
                    showSuccess('连接测试成功！');
                } else {
                    showError(response?.message || '连接测试失败');
                }
            } catch (error) {
                showError('连接测试失败: ' + error.message);
            }
        };

        const saveDatasource = async () => {
            // 验证必填字段
            if (!currentDatasource.name || !currentDatasource.host || !currentDatasource.port ||
                !currentDatasource.database || !currentDatasource.username || !currentDatasource.password) {
                showError('请填写所有必填字段');
                return;
            }

            try {
                savingDatasource.value = true;
                let response;
                
                // 如果用户已有数据源，先删除旧的再创建新的（实现单数据源限制）
                if (!currentDatasource.id && currentUserDataSource.value) {
                    await apiRequest(`${DATASOURCE_API}/${currentUserDataSource.value.id}`, {
                        method: 'DELETE'
                    });
                }
                
                if (currentDatasource.id) {
                    // 更新数据源
                    response = await apiRequest(`${DATASOURCE_API}/${currentDatasource.id}`, {
                        method: 'PUT',
                        body: JSON.stringify(currentDatasource)
                    });
                } else {
                    // 创建数据源
                    response = await apiRequest(DATASOURCE_API, {
                        method: 'POST',
                        body: JSON.stringify(currentDatasource)
                    });
                }
                
                if (response.success || response) {
                    showSuccess(currentDatasource.id ? '数据源更新成功' : '数据源创建成功');
                    // 刷新数据源列表
                    await loadDataSources();
                    
                    // 使用nextTick确保UI响应式更新
                    await nextTick();
                    
                    // 确保当前数据源已正确设置并被选中
                    if (currentUserDataSource.value) {
                        console.log('数据源已保存并刷新，当前数据源:', currentUserDataSource.value.name);
                        // 明确地选中并激活新保存的数据源
                        useDataSource(currentUserDataSource.value.id);
                        
                        // 额外等待一下，确保数据源激活完成
                        await nextTick();
                        
                        // 显式检查连接状态
                        if (!connectionStatus.value) {
                            console.log('正在重新检查连接状态...');
                            await checkConnection();
                        }
                        
                        // 如果连接成功，可以提示用户
                        if (connectionStatus.value) {
                            showInfo('数据源已成功连接');
                        }
                    }
                    
                    showDatasourceModal.value = false;
                    editingDatasource.value = false;
                    resetDatasourceForm();
                } else {
                    showError(currentDatasource.id ? '数据源更新失败' : '数据源创建失败');
                }
            } catch (error) {
                showError((currentDatasource.id ? '更新' : '创建') + '数据源失败: ' + error.message);
            } finally {
                savingDatasource.value = false;
            }
        };

        const deleteDataSource = async (id, name) => {
            try {
                await ElMessageBox.confirm(
                    `确定要删除数据源 "${name}" 吗？删除后将无法恢复。`,
                    '确认删除',
                    {
                        confirmButtonText: '确定',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }
                );

                const response = await apiRequest(`${DATASOURCE_API}/${id}`, {
                    method: 'DELETE'
                });
                
                if (response.success || response) {
                    showSuccess('数据源删除成功');
                    await loadDataSources();
                    
                    // 如果删除的是当前选中的数据源，切换到其他数据源
                    if (selectedDataSourceId.value === id && availableDataSources.value.length > 0) {
                        selectedDataSourceId.value = availableDataSources.value[0].id;
                        onDataSourceChange();
                    }
                } else {
                    showError('数据源删除失败');
                }
            } catch (error) {
                if (error !== 'cancel') {
                    showError('删除数据源失败: ' + error.message);
                }
            }
        };

        const useDataSource = (id) => {
            selectedDataSourceId.value = id;
            onDataSourceChange();
            closeDatasourceManager();
        };

        const onDataSourceChange = async () => {
            console.log('onDataSourceChange执行开始，selectedDataSourceId:', selectedDataSourceId.value);

            if (selectedDataSourceId.value) {
                console.log('开始处理选中的数据源:', selectedDataSourceId.value);

                // 清空之前的数据
                schema.tables = [];
                schema.totalColumns = 0;
                showResults.value = false;
                generatedSql.value = '';
                queryResults.value = null;

                // 设置当前数据库信息
                console.log('查找数据源信息，availableDataSources长度:', availableDataSources.value.length);
                const ds = availableDataSources.value.find(item => item.id === selectedDataSourceId.value);

                if (ds) {
                    console.log('找到数据源:', ds.name, '类型:', ds.type);
                    // 使用数据源的真实数据库名，而不是数据源名称
                    currentDatabase.value = ds.database;
                    console.log('设置当前数据库名:', currentDatabase.value);

                    // 直接检查连接状态，不需要重复激活（因为数据源在启动时已经初始化）
                    try {
                        console.log('直接检查数据源连接状态，调用API:', `${DATASOURCE_API}/${selectedDataSourceId.value}/status`);

                        const statusResponse = await fetch(`${DATASOURCE_API}/${selectedDataSourceId.value}/status`, {
                            method: 'GET',
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        });

                        console.log('数据源状态API响应状态:', statusResponse.status, statusResponse.statusText);
                        const statusData = await statusResponse.json();
                        console.log('数据源状态API响应数据:', statusData);

                        // 检查连接状态
                        if (statusResponse.ok && statusData.success && statusData.connected) {
                            connectionStatus.value = true;
                            console.log('数据源连接正常，设置connectionStatus为true');
                            console.log('连接成功，开始加载数据库结构...');
                            await loadSchema();
                        } else {
                            connectionStatus.value = false;
                            console.log('数据源连接失败，设置connectionStatus为false');
                            console.warn('连接失败，跳过加载数据库结构');
                        }
                    } catch (error) {
                        console.error('检查数据源状态异常:', error);
                        connectionStatus.value = false;
                        showError('检查数据源状态出错: ' + error.message);
                    }
                } else {
                    console.error('未找到ID为', selectedDataSourceId.value, '的数据源');
                    showError('未找到选择的数据源');
                }
            } else {
                console.log('没有选中数据源，执行清理工作');
                // 没有选中数据源时的清理工作
                schema.tables = [];
                currentDatabase.value = '';
                connectionStatus.value = false;
            }

            console.log('onDataSourceChange执行结束');
        };

        const getDataSourceTypeClass = (type) => {
            const typeClasses = {
                'mysql': 'badge-primary',
                'postgresql': 'badge-info',
                'oracle': 'badge-warning',
                'sqlserver': 'badge-success',
                'h2': 'badge-secondary'
            };
            return typeClasses[type] || 'badge-default';
        };

        const refreshDataSources = () => {
            loadDataSources();
        };

        // API 请求封装
        const apiRequest = async (url, options = {}) => {
            try {
                console.log('发送API请求:', url);
                const defaultOptions = {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                };

                const response = await fetch(url, { ...defaultOptions, ...options });
                console.log('收到响应状态:', response.status, response.statusText);

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                const responseText = await response.text();
                console.log('原始响应内容:', responseText);

                let data;
                try {
                    data = JSON.parse(responseText);
                    console.log('解析后的JSON数据:', data);
                } catch (parseError) {
                    console.error('JSON解析失败:', parseError);
                    console.error('响应内容:', responseText);
                    throw new Error('JSON解析失败: ' + parseError.message);
                }

                // 某些端点直接返回数据，不包含success字段
                // 比如 /server-info, /tools 等
                const isDirectResponse = url.includes('/server-info') ||
                                       url.includes('/tools') ||
                                       !data.hasOwnProperty('success');

                if (!isDirectResponse && !data.success) {
                    throw new Error(data.message || data.error || '请求失败');
                }

                return data;
            } catch (error) {
                console.error('API请求失败:', error);
                throw error;
            }
        };

        // 检查数据库连接
        const checkConnection = async () => {
            checking.value = true;
            console.log('检查数据库连接开始，selectedDataSourceId:', selectedDataSourceId.value);
            try {
                // 只有在有选中的数据源时才检查连接
                if (selectedDataSourceId.value) {
                    console.log('开始调用数据源状态检查API，ID:', selectedDataSourceId.value);

                    // 调用新的数据源状态检查API
                    const response = await fetch(`${DATASOURCE_API}/${selectedDataSourceId.value}/status`);
                    console.log('API响应状态:', response.status, response.statusText);

                    if (response.ok) {
                        const data = await response.json();
                        console.log('API响应数据:', data);

                        if (data.success && data.connected) {
                            connectionStatus.value = true;
                            console.log('设置connectionStatus为true');
                            showSuccess(data.message || '数据库连接正常');
                        } else {
                            connectionStatus.value = false;
                            console.log('设置connectionStatus为false');
                            showError(data.message || '数据库连接失败');
                        }
                    } else {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                } else {
                    console.log('没有选中的数据源，无法检查连接');
                    connectionStatus.value = false;
                    showInfo('请先配置数据源');
                }
            } catch (error) {
                console.error('数据库连接检查异常:', error);
                connectionStatus.value = false;
                console.log('设置connectionStatus为false');
                currentDatabase.value = '';
                showError('数据库连接失败: ' + error.message);
            } finally {
                checking.value = false;
                console.log('检查数据库连接结束，connectionStatus:', connectionStatus.value);
            }
        };

        // 从响应消息中提取数据库名称
        const extractDatabaseName = (message) => {
            if (!message || typeof message !== 'string') {
                return '未知';
            }
            const match = message.match(/当前数据库:\s*(\w+)/);
            return match ? match[1] : '未知';
        };

        // 加载数据库结构
        const loadSchema = async () => {
            console.log('loadSchema函数执行，当前状态：', {
                selectedDataSourceId: selectedDataSourceId.value,
                connectionStatus: connectionStatus.value,
                currentDatabase: currentDatabase.value
            });
            
            if (!selectedDataSourceId.value) {
                console.warn('没有选中的数据源，跳过加载结构');
                showError('请先选择数据源');
                return;
            }
            
            // 如果连接状态未确认，先尝试检查连接，但即使失败也继续执行
            if (!connectionStatus.value) {
                showInfo('正在检查数据库连接...');
                try {
                    await checkConnection();
                    console.log('连接检查完成，结果:', connectionStatus.value);
                } catch (checkError) {
                    console.error('连接检查出错，但继续尝试加载结构:', checkError);
                    // 即使连接检查失败也继续执行
                }
            }

            loadingSchema.value = true;
            try {
                console.log('开始请求数据库结构...');
                
                // 使用直接的fetch API以获取更完整的响应信息
                const response = await fetch(`${DATASOURCE_API}/${selectedDataSourceId.value}/schema`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                console.log('API响应状态:', response.status);
                const responseData = await response.json();
                console.log('API响应数据:', responseData);

                // 更宽松的成功判断条件
                if (response.ok || responseData.success || responseData.tables || responseData.data) {
                    // 处理可能的不同响应格式
                    if (responseData.tables) {
                        // 如果响应直接包含tables字段，直接使用
                        schema.tables = responseData.tables || [];
                        schema.totalColumns = schema.tables.reduce((sum, table) => sum + (table.columns?.length || 0), 0);

                        // 自动展开第一个表
                        if (schema.tables.length > 0) {
                            schema.tables[0].expanded = true;
                        }
                        showSuccess('数据库结构加载完成');
                    } else if (responseData.data && responseData.data.tables) {
                        // 如果响应包含data.tables字段（当前API格式），直接使用
                        schema.tables = responseData.data.tables || [];
                        schema.totalColumns = schema.tables.reduce((sum, table) => sum + (table.columns?.length || 0), 0);

                        // 自动展开第一个表
                        if (schema.tables.length > 0) {
                            schema.tables[0].expanded = true;
                        }
                        showSuccess('数据库结构加载完成');
                    } else if (responseData.content) {
                        // 如果响应包含content字段，使用parseSchemaFromDescription解析
                        parseSchemaFromDescription(responseData.content);
                        showSuccess('数据库结构加载完成');
                    } else {
                        console.warn('未知的响应格式，responseData:', responseData);
                        // 对于空的成功响应，不抛出错误，只是不加载表结构
                        schema.tables = [];
                        schema.totalColumns = 0;
                        showInfo('数据库结构为空或格式不匹配');
                    }
                    
                    // 加载成功后，强制将连接状态设为true
                    connectionStatus.value = true;
                    console.log('加载成功，强制设置连接状态为true');
                } else {
                    throw new Error(responseData.error || responseData.message || '获取数据库结构失败');
                }
            } catch (error) {
                console.error('加载数据库结构失败:', error);
                showError('加载数据库结构失败: ' + error.message);
                
                // 失败时也尝试重新检查连接状态
                try {
                    console.log('加载失败，重新检查连接状态...');
                    await checkConnection();
                } catch (checkError) {
                    console.error('连接状态检查失败:', checkError);
                }
            } finally {
                loadingSchema.value = false;
            }
        };

        // 解析数据库结构描述
        const parseSchemaFromDescription = (description) => {
            if (!description || typeof description !== 'string') {
                console.warn('解析数据库结构失败: description 参数无效', description);
                return;
            }
            console.log('开始解析数据库结构描述，长度:', description.length);
            const lines = description.split('\n');
            console.log('分割后的行数:', lines.length);
            const tables = [];
            let currentTable = null;
            let totalColumns = 0;

            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                
                // 解析表名 (表名: xxx 或 ### 表名)
                if (line.startsWith('表名: ') || line.startsWith('### ')) {
                    if (currentTable) {
                        tables.push(currentTable);
                        console.log('添加表:', currentTable.name, '列数:', currentTable.columns.length);
                    }
                    const tableName = line.startsWith('表名: ') ? line.substring(4) : line.substring(4);
                    currentTable = {
                        name: tableName,
                        comment: '',
                        columns: [],
                        foreignKeys: []
                    };
                    console.log('发现新表:', currentTable.name);
                }
                
                // 解析表注释 (说明: xxx 或 **说明**:)
                else if ((line.startsWith('说明: ') || line.startsWith('**说明**:')) && currentTable) {
                    currentTable.comment = line.startsWith('说明: ') ? line.substring(4).trim() : line.substring(6).trim();
                }
                
                // 解析字段信息 (  - xxx 或 - `xxx`)
                else if (line.startsWith('  - ') || line.startsWith('- `')) {
                    if (currentTable) {
                        const column = parseColumnLine(line);
                        if (column) {
                            currentTable.columns.push(column);
                            totalColumns++;
                        }
                    }
                }
                
                // 解析关联关系
                else if (line.startsWith('- ') && currentTable && 
                         lines[i-1] && lines[i-1].includes('**关联关系**:')) {
                    currentTable.foreignKeys.push({
                        relationshipDescription: line.substring(2)
                    });
                }
            }

            // 添加最后一个表
            if (currentTable) {
                tables.push(currentTable);
                console.log('添加最后一个表:', currentTable.name, '列数:', currentTable.columns.length);
            }

            console.log('解析完成，总表数:', tables.length, '总列数:', totalColumns);
            schema.tables = tables;
            schema.totalColumns = totalColumns;
            console.log('更新schema后，schema.tables.length:', schema.tables.length);
        };

        // 解析字段行信息
        const parseColumnLine = (line) => {
            // 格式1: - `column_name` type [主键] [非空] [自增] - comment
            // 格式2:   - column_name (TYPE) [主键] - comment
            let match = line.match(/^[\s-]+`([^`]+)`\s+([^[]+)/);
            if (!match) {
                match = line.match(/^[\s-]+(\w+)\s+\(([^)]+)\)/);
            }
            if (!match) return null;

            const name = match[1];
            const typePart = match[2].trim();
            
            const column = {
                name: name,
                type: typePart.split(' ')[0],
                displayType: typePart,
                primaryKey: line.includes('[主键]'),
                nullable: !line.includes('[非空]'),
                autoIncrement: line.includes('[自增]'),
                comment: ''
            };

            // 提取注释
            const commentMatch = line.match(/ - (.+)$/);
            if (commentMatch) {
                column.comment = commentMatch[1];
            }

            return column;
        };

        // 切换表的展开/收起状态
        const toggleTable = (tableName) => {
            const index = activeTableNames.value.indexOf(tableName);
            if (index > -1) {
                activeTableNames.value.splice(index, 1);
            } else {
                activeTableNames.value.push(tableName);
            }
        };

        // 插入列名到查询框
        const insertColumnName = (tableName, columnName) => {
            const insertion = `${tableName}.${columnName}`;
            userQuery.value += (userQuery.value ? ' ' : '') + insertion;
            showInfo(`已插入: ${insertion}`);
        };

        // 滚动到顶部
        const scrollToTop = () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        };

        // 转换为SQL
        const convertToSql = async () => {
            if (!userQuery.value.trim()) {
                showError('请输入查询内容');
                return;
            }

            if (!connectionStatus.value) {
                showError('请先检查数据库连接');
                return;
            }

            if (executing.value || converting.value) {
                showError('正在处理查询，请等待完成');
                return;
            }

            converting.value = true;
            try {
                const response = await fetch(`${API_BASE}/text2sql`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        prompt: userQuery.value,
                        context: '',
                        dataSourceId: selectedDataSourceId.value || currentUserDataSource.value?.id || null
                    })
                });

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.error || '转换失败');
                }

                rawResponse.value = data;
                const responseData = data.content || data.data || data.result;
                console.log('Text2SQL API响应数据:', responseData);
                parseTextToSqlResponse(responseData);
                showResults.value = true;
                activeTab.value = 'sql';
                showSuccess('SQL生成成功');

            } catch (error) {
                showError('SQL生成失败: ' + error.message);
            } finally {
                converting.value = false;
            }
        };

        // 解析Text2SQL响应
        const parseTextToSqlResponse = (responseText) => {
            const lines = responseText.split('\n');
            let inSqlBlock = false;
            let inExplanation = false;
            let sqlLines = [];
            let explanationLines = [];

            for (const line of lines) {
                if (line.includes('**生成的SQL语句:**')) {
                    inSqlBlock = false;
                    inExplanation = false;
                    continue;
                }
                
                if (line.includes('```sql')) {
                    inSqlBlock = true;
                    continue;
                }
                
                if (line.includes('```') && inSqlBlock) {
                    inSqlBlock = false;
                    continue;
                }
                
                if (line.includes('**查询说明:**')) {
                    inSqlBlock = false;
                    inExplanation = true;
                    continue;
                }

                if (inSqlBlock && line.trim()) {
                    sqlLines.push(line);
                }
                
                if (inExplanation && line.trim()) {
                    explanationLines.push(line);
                }
            }

            generatedSql.value = sqlLines.join('\n').trim();
            sqlExplanation.value = explanationLines.join('\n').trim();
        };

        // 执行SQL
        const executeSql = async () => {
            if (!generatedSql.value) {
                showError('没有可执行的SQL语句');
                return;
            }

            executing.value = true;
            try {
                const response = await fetch(`${API_BASE}/execute-sql`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sql: generatedSql.value
                    })
                });

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.error || '转换失败');
                }

                const responseData = data.data || data.result;
                parseExecuteResponse(responseData);
                activeTab.value = 'results';
                showSuccess('查询执行成功');

            } catch (error) {
                showError('查询执行失败: ' + error.message);
            } finally {
                executing.value = false;
            }
        };

        // 生成并执行
        const queryAndExecute = async () => {
            if (!userQuery.value.trim()) {
                showError('请输入查询内容');
                return;
            }

            if (!connectionStatus.value) {
                showError('请先检查数据库连接');
                return;
            }

            if (converting.value || executing.value) {
                showError('正在处理查询，请等待完成');
                return;
            }

            executing.value = true;
            try {
                const formData = new FormData();
                formData.append('query', userQuery.value);

                const response = await fetch(`${API_BASE}/query-and-execute`, {
                    method: 'POST',
                    body: formData
                });

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.error || '转换失败');
                }

                rawResponse.value = data;
                const responseData = data.data || data.result;
                console.log('查询并执行API响应数据:', responseData);
                parseQueryAndExecuteResponse(responseData);
                showResults.value = true;
                activeTab.value = 'results';
                showSuccess('查询执行完成');

            } catch (error) {
                showError('查询执行失败: ' + error.message);
            } finally {
                executing.value = false;
            }
        };

        // 解析查询执行响应
        const parseExecuteResponse = (responseText) => {
            parseExecutionResults(responseText);
        };

        // 解析查询并执行响应
        const parseQueryAndExecuteResponse = (responseText) => {
            const lines = responseText.split('\n');
            let inSqlBlock = false;
            let inExplanation = false;
            let inResults = false;
            let sqlLines = [];
            let explanationLines = [];
            let resultLines = [];

            for (const line of lines) {
                if (line.includes('**生成的SQL:**')) {
                    inSqlBlock = false;
                    inExplanation = false;
                    inResults = false;
                    continue;
                }
                
                if (line.includes('```sql')) {
                    inSqlBlock = true;
                    continue;
                }
                
                if (line.includes('```') && inSqlBlock) {
                    inSqlBlock = false;
                    continue;
                }
                
                if (line.includes('**查询说明:**')) {
                    inSqlBlock = false;
                    inExplanation = true;
                    inResults = false;
                    continue;
                }
                
                if (line.includes('**执行结果:**')) {
                    inSqlBlock = false;
                    inExplanation = false;
                    inResults = true;
                    continue;
                }

                if (inSqlBlock && line.trim()) {
                    sqlLines.push(line);
                }
                
                if (inExplanation && line.trim() && !line.includes('**执行结果:**')) {
                    explanationLines.push(line);
                }
                
                if (inResults && line.trim()) {
                    resultLines.push(line);
                }
            }

            generatedSql.value = sqlLines.join('\n').trim();
            sqlExplanation.value = explanationLines.join('\n').trim();
            parseExecutionResults(resultLines.join('\n'));
        };

        // 解析执行结果
        const parseExecutionResults = (resultText) => {
            const lines = resultText.split('\n');
            
            // 提取统计信息
            let totalRows = 0;
            let executionTime = 0;
            let truncated = false;

            for (const line of lines) {
                if (line.includes('**查询统计:**')) {
                    const match = line.match(/返回 (\d+) 行数据，执行耗时 (\d+) ms/);
                    if (match) {
                        totalRows = parseInt(match[1]);
                        executionTime = parseInt(match[2]);
                    }
                    truncated = line.includes('结果已截断');
                    break;
                }
            }

            if (totalRows === 0) {
                queryResults.value = {
                    columns: [],
                    rows: [],
                    totalRows: 0,
                    executionTime: executionTime,
                    truncated: false
                };
                return;
            }

            // 解析表格数据
            const tableStartIndex = lines.findIndex(line => line.includes('**查询结果:**'));
            if (tableStartIndex === -1) {
                queryResults.value = null;
                return;
            }

            const tableLines = lines.slice(tableStartIndex + 1);
            
            // 查找表头
            let headerLine = null;
            let separatorIndex = -1;
            
            for (let i = 0; i < tableLines.length; i++) {
                if (tableLines[i].startsWith('|') && tableLines[i].includes('|')) {
                    if (headerLine === null) {
                        headerLine = tableLines[i];
                    } else if (tableLines[i].includes('---|')) {
                        separatorIndex = i;
                        break;
                    }
                }
            }

            if (!headerLine || separatorIndex === -1) {
                queryResults.value = null;
                return;
            }

            // 解析列名
            const columns = headerLine.split('|')
                .map(col => col.trim())
                .filter(col => col !== '');

            // 解析数据行
            const rows = [];
            for (let i = separatorIndex + 1; i < tableLines.length; i++) {
                const line = tableLines[i];
                if (!line.startsWith('|') || line.includes('...')) break;
                
                const rowData = line.split('|')
                    .map(cell => cell.trim())
                    .filter((cell, index) => index > 0 && index <= columns.length)
                    .map(cell => cell === 'NULL' ? null : cell);
                    
                if (rowData.length === columns.length) {
                    rows.push(rowData);
                }
            }

            queryResults.value = {
                columns: columns,
                rows: rows,
                totalRows: totalRows,
                executionTime: executionTime,
                truncated: truncated
            };
        };

        // 复制SQL
        const copySql = async () => {
            if (!generatedSql.value) {
                showError('没有可复制的SQL语句');
                return;
            }

            try {
                await navigator.clipboard.writeText(generatedSql.value);
                showSuccess('SQL已复制到剪贴板');
            } catch (error) {
                console.error('复制失败:', error);
                showError('复制失败，请手动复制');
            }
        };

        // 格式化解释文本
        const formatExplanation = (text) => {
            if (!text) return '';
            return text.replace(/\n/g, '<br>');
        };

        // 格式化单元格值
        const formatCellValue = (value) => {
            if (value === null || value === undefined) {
                return 'NULL';
            }
            
            if (typeof value === 'string' && value.length > 50) {
                return value.substring(0, 47) + '...';
            }
            
            return value.toString();
        };

        // 清空所有内容
        const clearAll = () => {
            userQuery.value = '';
            generatedSql.value = '';
            sqlExplanation.value = '';
            queryResults.value = null;
            rawResponse.value = null;
            showResults.value = false;
            feedbackSubmitted.value = false;
            showInfo('已清空所有内容');
        };

        // 提交简单反馈（正确）
        const submitFeedback = async (type) => {
            if (type === 'correct') {
                await submitUserFeedback(true, '', '');
            }
        };

        // 显示反馈对话框
        const showFeedbackDialog = (type) => {
            feedbackType.value = type;
            correctedSql.value = '';
            feedbackDescription.value = '';
            showFeedbackModal.value = true;
        };

        // 关闭反馈对话框
        const closeFeedbackDialog = () => {
            showFeedbackModal.value = false;
            feedbackType.value = '';
            correctedSql.value = '';
            feedbackDescription.value = '';
        };

        // 提交详细反馈
        const submitDetailedFeedback = async () => {
            const isCorrect = feedbackType.value === 'correction' ? false : false;
            const correctionSql = feedbackType.value === 'correction' ? correctedSql.value : '';
            
            await submitUserFeedback(isCorrect, correctionSql, feedbackDescription.value);
            closeFeedbackDialog();
        };

        // 通用反馈提交方法
        const submitUserFeedback = async (isCorrect, correctionSql, description) => {
            if (!userQuery.value || !generatedSql.value) {
                showError('缺少必要的查询数据');
                return;
            }

            submittingFeedback.value = true;
            try {
                const formData = new FormData();
                formData.append('userQuery', userQuery.value);
                formData.append('generatedSql', generatedSql.value);
                formData.append('isCorrect', isCorrect.toString());
                
                if (correctionSql) {
                    formData.append('correctedSql', correctionSql);
                }

                const response = await fetch(`${API_BASE}/user-feedback`, {
                    method: 'POST',
                    body: formData
                });

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.error || '转换失败');
                }

                feedbackSubmitted.value = true;
                showSuccess(isCorrect ? '感谢您的正面反馈！' : '感谢您的反馈，AI正在学习改进！');

                // 3秒后重置反馈状态
                setTimeout(() => {
                    feedbackSubmitted.value = false;
                }, 3000);

            } catch (error) {
                showError('反馈提交失败: ' + error.message);
            } finally {
                submittingFeedback.value = false;
            }
        };

        // 组件挂载时执行
        onMounted(() => {
            loadDataSources();
        });

        // 返回响应式数据和方法
        return {
            // 数据
            connectionStatus,
            connected: computed(() => connectionStatus.value), // 为了兼容HTML模板中的connected引用
            checking,
            loadingSchema,
            converting,
            executing,
            userQuery,
            currentDatabase,
            schema,
            activeTableNames,
            showResults,
            activeTab,
            generatedSql,
            sqlExplanation,
            queryResults,
            rawResponse,
            exampleQueries,
            
            // 反馈相关数据
            feedbackSubmitted,
            submittingFeedback,
            showFeedbackModal,
            feedbackType,
            correctedSql,
            feedbackDescription,
            
            // 数据源管理
            showDatasourceModal,
            editingDatasource,
            loadingDataSources,
            savingDatasource,
            availableDataSources,
            selectedDataSourceId,
            currentDatasource,
            currentUserDataSource,
            showDatasourceDropdown,
            
            // 方法
            checkConnection,
            loadSchema,
            toggleTable,
            insertColumnName,
            convertToSql,
            executeSql,
            queryAndExecute,
            copySql,
            formatExplanation,
            formatCellValue,
            clearAll,
            scrollToTop,
            
            // 反馈相关方法
            submitFeedback,
            showFeedbackDialog,
            closeFeedbackDialog,
            submitDetailedFeedback,
            
            // 数据源管理方法
            openDataSourceManager,
            closeDatasourceManager,
            startCreateDatasource,
            editDataSource,
            cancelEdit,
            testCurrentConnection,
            testDataSourceConnection,
            saveDatasource,
            deleteDataSource,
            useDataSource,
            onDataSourceChange,
            getDataSourceTypeClass,
            refreshDataSources,
            toggleDatasourceDropdown,
            editCurrentDatasource,
            testCurrentUserConnection,
            deleteCurrentDatasource,
            getDatabaseTypeName
        };
    }
});

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app');