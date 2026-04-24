import { Bell, Building2, FileText, LayoutDashboard, Workflow } from 'lucide-react'
import { NavLink, Route, Routes } from 'react-router-dom'

const cards = [
  {
    title: '组织与权限',
    description: '统一组织树、账号、角色与数据权限治理入口。',
    icon: Building2,
  },
  {
    title: '流程与表单',
    description: '围绕流程发起、待办审批与表单渲染能力构建主工作区。',
    icon: Workflow,
  },
  {
    title: '门户与工作台',
    description: '聚合待办、公告、消息和业务卡片，形成统一门户首页。',
    icon: LayoutDashboard,
  },
  {
    title: '内容与知识',
    description: '承载公告、制度、知识库与全文检索的统一入口。',
    icon: FileText,
  },
  {
    title: '消息中心',
    description: '汇聚站内消息、提醒、订阅和多渠道通知状态。',
    icon: Bell,
  },
]

function HomePage() {
  return (
    <div className="page-shell">
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">HJO2OA Portal Web</span>
          <h1>React 门户骨架已经就位</h1>
          <p>
            当前前端基线已经切换为 React + TypeScript + Vite，可直接在此基础上继续拆分门户端、管理端与移动端应用。
          </p>
          <div className="hero-actions">
            <a className="primary-action" href="#capabilities">
              查看平台能力
            </a>
            <NavLink className="secondary-action" to="/roadmap">
              查看实施路线
            </NavLink>
          </div>
        </div>
        <div className="hero-panel">
          <div className="stat-card">
            <span>数据库基线</span>
            <strong>SQL Server 2017</strong>
          </div>
          <div className="stat-card">
            <span>迁移方案</span>
            <strong>Flyway</strong>
          </div>
          <div className="stat-card">
            <span>前端框架</span>
            <strong>React 18</strong>
          </div>
        </div>
      </section>

      <section className="capability-section" id="capabilities">
        <div className="section-heading">
          <span>平台能力地图</span>
          <h2>围绕 O2OA 类协同办公平台的一期核心能力展开</h2>
        </div>
        <div className="capability-grid">
          {cards.map((card) => {
            const Icon = card.icon
            return (
              <article className="capability-card" key={card.title}>
                <div className="capability-icon">
                  <Icon size={20} />
                </div>
                <h3>{card.title}</h3>
                <p>{card.description}</p>
              </article>
            )
          })}
        </div>
      </section>
    </div>
  )
}

function RoadmapPage() {
  return (
    <div className="page-shell compact-shell">
      <section className="section-card">
        <span className="eyebrow">Implementation Roadmap</span>
        <h2>建议的 React 端实施顺序</h2>
        <ol className="roadmap-list">
          <li>先沉淀应用壳、路由、鉴权、主题与国际化基础设施。</li>
          <li>再建设门户首页、待办中心、消息中心等平台级聚合页面。</li>
          <li>随后按领域拆分流程、内容、组织权限与协同办公子应用。</li>
          <li>最后补齐设计器类重交互能力与移动端 H5 体验收敛。</li>
        </ol>
      </section>
    </div>
  )
}

export default function App() {
  return (
    <div className="app-frame">
      <header className="topbar">
        <div>
          <span className="brand-mark">HJO2OA</span>
          <p className="brand-subtitle">React Portal Starter</p>
        </div>
        <nav className="topnav">
          <NavLink to="/" end>
            首页
          </NavLink>
          <NavLink to="/roadmap">路线</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/roadmap" element={<RoadmapPage />} />
        </Routes>
      </main>
    </div>
  )
}
