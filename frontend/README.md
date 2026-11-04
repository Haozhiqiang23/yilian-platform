# 医联智诊平台 · 前端骨架

> 技术栈：Vue 3 + Vite + Vue Router 4 + Pinia + Axios
> 设计系统：医疗薄荷青 + 玻璃拟态（见 `outputs/design-system/design-system/医联智诊平台/MASTER.md`）

## 本地运行

```bash
npm install        # 安装依赖（国内镜像已配置）
npm run dev        # 启动开发服务器 http://localhost:5173
npm run build      # 生产构建
```

> 提示：`dev` 启动后浏览器打开 `http://localhost:5173`，登录页演示模式可任选角色（患者/医生/管理员）进入对应端。

## 目录结构

```
src/
├── api/            # 接口层（按模块拆分，与页面一一对应）
│   ├── auth.js     # 认证：登录/注册/验证码
│   └── patient.js  # 患者端：医生/号源/预约/支付/签到/排队/记录/处方/报告
├── assets/
│   ├── tokens.css  # 设计令牌（色彩/圆角/阴影/字体，全站唯一事实来源）
│   └── base.css    # 全局重置与工具类
├── components/
│   ├── AppLogo.vue         # 品牌 Logo
│   └── PlaceholderPage.vue # 占位页（待开发页面）
├── layouts/
│   ├── PatientLayout.vue   # 患者端：顶部栏 + 底部 Tab（移动优先）
│   ├── DoctorLayout.vue    # 医生端：左侧菜单（桌面优先）
│   └── AdminLayout.vue     # 管理员端：左侧菜单（桌面优先）
├── router/
│   ├── index.js            # 三端路由表（全部懒加载）
│   └── guards.js           # 登录态 + 角色分区守卫
├── stores/
│   └── user.js             # 用户会话（token/角色/落地页映射）
├── utils/
│   └── request.js          # Axios 封装（统一解包/401/403）
└── views/
    ├── auth/      # 登录 / 注册（已实现）
    ├── patient/   # 首页（已实现），医生列表/预约/记录/个人信息（占位）
    ├── doctor/    # 工作台/患者列表/报告/个人信息（占位）
    └── admin/     # 首页/用户/医生/号源/退款/日志（占位）
```

## 联调说明

- 后端地址：`http://localhost:8080`，vite.config.js 中已预留 `/api` 代理（取消注释即可）
- 统一返回结构：`{ code, message, data }`，`code === 0 || 200` 视为成功
- 登录页已内置**演示模式**（任意密码 + 角色选择即可登录），联调时替换 `LoginView.vue` 中注释标注的正式请求即可

## 已实现 / 待实现

| 模块 | 状态 |
|------|------|
| 工程骨架、路由守卫、接口层 | ✅ |
| 登录页（含演示角色）、注册页（前端校验） | ✅ |
| 患者首页（渐变头部/搜索/待支付横幅/快捷服务/常用医生） | ✅ |
| 医生端 / 管理员端布局壳 | ✅ |
| 其余 20+ 业务页面 | 🚧 占位（按《前端页面设计方案》逐页实现） |