// Vue.js 应用主逻辑
const { createApp, ref, reactive, computed, onMounted, nextTick } = Vue;
const { ElMessage, ElMessageBox, ElNotification } = ElementPlus;

// API 基础配置
const API_BASE = '/api/mcp';

// 创建 Vue 应用
const app = createApp({
    setup() {
        // 响应式数据
        const connectionStatus = ref(false);
        const checking = ref(false);
        const loadingSchema = ref(false);
        const converting = ref(false);
        const executing = ref(false);
        
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
                showClose: true
            });
            console.error(title + ':', message);
        };

        const showSuccess = (message) => {
            ElMessage({
                type: 'success',
                message: message,
                duration: 3000
            });
        };

        const showInfo = (message) => {
            ElMessage({
                type: 'info',
                message: message,
                duration: 3000
            });
        };

        // API 请求封装
        const apiRequest = async (url, options = {}) => {
            try {
                const defaultOptions = {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                };

                const response = await fetch(url, { ...defaultOptions, ...options });
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                const data = await response.json();
                
                if (!data.success) {
                    throw new Error(data.message || '请求失败');
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
            try {
                const response = await apiRequest(`${API_BASE}/server-info`);
                connectionStatus.value = true;
                currentDatabase.value = 'test'; // 暂时硬编码数据库名
                showSuccess('数据库连接正常');
                
                // 自动加载数据库结构（仅在首次连接或结构为空时加载）
                if (!schema.tables.length) {
                    console.log('检测到数据库结构为空，自动加载结构');
                    loadSchema();
                } else {
                    console.log('数据库结构已缓存，跳过自动加载');
                }
            } catch (error) {
                connectionStatus.value = false;
                currentDatabase.value = '';
                showError('数据库连接失败: ' + error.message);
            } finally {
                checking.value = false;
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
            if (!connectionStatus.value) {
                showError('请先检查数据库连接');
                return;
            }

            loadingSchema.value = true;
            try {
                console.log('开始请求数据库结构...');
                const response = await apiRequest(`${API_BASE}/database-schema`, {
                    method: 'GET'
                });
                console.log('API响应:', response);

                // 检查响应格式
                if (!response.success) {
                    throw new Error(response.error || '获取数据库结构失败');
                }

                // 解析结构信息
                parseSchemaFromDescription(response.content);
                showSuccess('数据库结构加载完成');
            } catch (error) {
                console.error('加载数据库结构失败:', error);
                showError('加载数据库结构失败: ' + error.message);
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
                
                // 解析表名 (### 表名)
                if (line.startsWith('### ')) {
                    if (currentTable) {
                        tables.push(currentTable);
                        console.log('添加表:', currentTable.name, '列数:', currentTable.columns.length);
                    }
                    currentTable = {
                        name: line.substring(4),
                        comment: '',
                        columns: [],
                        foreignKeys: []
                    };
                    console.log('发现新表:', currentTable.name);
                }
                
                // 解析表注释
                else if (line.startsWith('**说明**:') && currentTable) {
                    currentTable.comment = line.substring(6).trim();
                }
                
                // 解析字段信息
                else if (line.startsWith('- `') && currentTable) {
                    const column = parseColumnLine(line);
                    if (column) {
                        currentTable.columns.push(column);
                        totalColumns++;
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
            // 格式: - `column_name` type [主键] [非空] [自增] - comment
            const match = line.match(/^- `([^`]+)` ([^[]+)/);
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
                        context: ''
                    })
                });

                const data = await response.json();
                if (!data.success) {
                    throw new Error(data.error || '转换失败');
                }

                rawResponse.value = data;
                const responseData = data.data || data.result;
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
            // 自动检查连接
            checkConnection();
        });

        // 返回响应式数据和方法
        return {
            // 数据
            connectionStatus,
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
            submitDetailedFeedback
        };
    }
});

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app');