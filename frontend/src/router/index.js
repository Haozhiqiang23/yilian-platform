import { createRouter, createWebHistory } from 'vue-router'
import { setupGuards } from './guards'

/**
 * 三端一体路由表
 * 分区：/patient（患者） /doctor（医生） /admin（管理员）
 * 页面组件全部懒加载（按路由分包）
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { public: true, title: '注册' },
  },

  // ---------------- 患者端 /patient ----------------
  {
    path: '/patient',
    component: () => import('@/layouts/PatientLayout.vue'),
    meta: { role: 'patient' },
    children: [
      {
        path: 'home',
        name: 'patient-home',
        component: () => import('@/views/patient/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'doctors',
        name: 'patient-doctors',
        component: () => import('@/views/patient/DoctorsView.vue'),
        meta: { title: '医生列表' },
      },
      {
        path: 'doctors/:id',
        name: 'patient-doctor-detail',
        component: () => import('@/views/patient/DoctorDetailView.vue'),
        meta: { title: '医生详情' },
      },
      {
        path: 'appointments',
        name: 'patient-appointments',
        component: () => import('@/views/patient/AppointmentsView.vue'),
        meta: { title: '我的预约' },
      },
      {
        path: 'appointments/confirm',
        name: 'patient-appointment-confirm',
        component: () => import('@/views/patient/AppointmentConfirmView.vue'),
        meta: { title: '预约确认' },
      },
      {
        path: 'appointments/:id/pay',
        name: 'patient-payment',
        component: () => import('@/views/patient/PaymentView.vue'),
        meta: { title: '支付' },
      },
      {
        path: 'appointments/:id',
        name: 'patient-appointment-detail',
        component: () => import('@/views/patient/AppointmentDetailView.vue'),
        meta: { title: '预约详情' },
      },
      {
        path: 'queue/:appointmentId',
        name: 'patient-queue',
        component: () => import('@/views/patient/QueueView.vue'),
        meta: { title: '排队等待' },
      },
      {
        path: 'records',
        name: 'patient-records',
        component: () => import('@/views/patient/RecordsView.vue'),
        meta: { title: '诊疗记录' },
      },
      {
        path: 'messages',
        name: 'patient-messages',
        component: () => import('@/views/patient/MessagesView.vue'),
        meta: { title: '消息通知' },
      },
      {
        path: 'prescriptions/:id',
        name: 'patient-prescription-detail',
        component: () => import('@/views/patient/PrescriptionDetailView.vue'),
        meta: { title: '处方详情' },
      },
      {
        path: 'reports/:id',
        name: 'patient-report-detail',
        component: () => import('@/views/patient/ReportDetailView.vue'),
        meta: { title: '检查报告' },
      },
      {
        path: 'profile',
        name: 'patient-profile',
        component: () => import('@/views/patient/ProfileView.vue'),
        meta: { title: '个人信息' },
      },
    ],
  },

  // ---------------- 医生端 /doctor ----------------
  {
    path: '/doctor',
    component: () => import('@/layouts/DoctorLayout.vue'),
    meta: { role: 'doctor' },
    children: [
      {
        path: 'dashboard',
        name: 'doctor-dashboard',
        component: () => import('@/views/doctor/DashboardView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'queues',
        name: 'doctor-queues',
        component: () => import('@/views/doctor/QueuesView.vue'),
        meta: { title: '患者列表' },
      },
      {
        path: 'reports',
        name: 'doctor-reports',
        component: () => import('@/views/doctor/ReportsView.vue'),
        meta: { title: '报告管理' },
      },
      {
        path: 'records/:appointmentId',
        name: 'doctor-record-editor',
        component: () => import('@/views/doctor/RecordEditorView.vue'),
        meta: { title: '诊疗记录' },
      },
      {
        path: 'profile',
        name: 'doctor-profile',
        component: () => import('@/views/doctor/ProfileView.vue'),
        meta: { title: '个人信息' },
      },
    ],
  },

  // ---------------- 管理员端 /admin ----------------
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { role: 'admin' },
    children: [
      {
        path: 'home',
        name: 'admin-home',
        component: () => import('@/views/admin/HomeView.vue'),
        meta: { title: '后台首页' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'doctors',
        name: 'admin-doctors',
        component: () => import('@/views/admin/DoctorsView.vue'),
        meta: { title: '医生管理' },
      },
      {
        path: 'schedules',
        name: 'admin-schedules',
        component: () => import('@/views/admin/SchedulesView.vue'),
        meta: { title: '号源管理' },
      },
      {
        path: 'refunds',
        name: 'admin-refunds',
        component: () => import('@/views/admin/RefundsView.vue'),
        meta: { title: '退款管理' },
      },
      {
        path: 'queues',
        name: 'admin-queues',
        component: () => import('@/views/admin/QueuesView.vue'),
        meta: { title: '排队管理' },
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/LogsView.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'config',
        name: 'admin-config',
        component: () => import('@/views/admin/ConfigView.vue'),
        meta: { title: '系统配置' },
      },
    ],
  },

  // 兜底：未匹配 → 登录页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

// 注册守卫
setupGuards(router)

// 页面标题
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 医联智诊平台` : '医联智诊平台'
})

export default router

