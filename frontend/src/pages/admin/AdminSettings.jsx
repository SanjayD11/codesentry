import React, { useState, useEffect, useCallback } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { getAllSettings, updateSetting } from '../../api/adminApi';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

const C = {
  primary:'#0058be', outline:'#c2c6d6', onSurface:'#111c2d', onVariant:'#424754',
  error:'#ba1a1a', errorContainer:'#ffdad6', tertiary:'#006947',
  primaryBg:'rgba(0,88,190,0.08)',
};

const CATEGORY_ICONS = {
  Security: 'security',
  Authentication: 'key',
  Scanning: 'shield',
  AI: 'smart_toy',
  Email: 'email',
  General: 'settings',
  Storage: 'folder',
};

function SettingRow({ setting, onSave }) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(setting.settingValue || '');
  const [saving, setSaving] = useState(false);
  const addToast = useToast();

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave(setting.settingKey, value);
      setEditing(false);
      addToast('Setting saved successfully', 'success');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to save setting', 'error');
    } finally { setSaving(false); }
  };

  const isBoolean = setting.valueType === 'BOOLEAN';
  const isReadOnly = !setting.editable;

  return (
    <div className="as-setting-row" style={{
      display:'flex', justifyContent:'space-between', alignItems:'center',
      padding:'14px 0', borderBottom:`1px solid #f0f3ff`, gap:14, minWidth:0
    }}>
      <div style={{ flex:1, minWidth:0 }}>
        <div style={{ display:'flex', alignItems:'center', gap:8, flexWrap:'wrap' }}>
          <code style={{ fontSize:12.5, fontFamily:'monospace', color:C.primary,
            background:C.primaryBg, padding:'2px 7px', borderRadius:5, wordBreak:'break-all' }}>
            {setting.settingKey}
          </code>
          {isReadOnly && (
            <span style={{ fontSize:11, color:C.onVariant, background:'#f0f3ff',
              padding:'2px 7px', borderRadius:5, fontWeight:600 }}>Read-only</span>
          )}
          <span style={{ fontSize:11, color:C.onVariant, background:'#f0f3ff',
            padding:'2px 7px', borderRadius:5 }}>{setting.valueType}</span>
        </div>
        <p style={{ margin:'6px 0 0', fontSize:13, color:C.onVariant, lineHeight:1.4 }}>{setting.description}</p>
        {setting.updatedBy && (
          <p style={{ margin:'3px 0 0', fontSize:11.5, color:'#9aa5b4' }}>
            Last updated by {setting.updatedBy}
          </p>
        )}
      </div>

      <div className="as-setting-controls" style={{ display:'flex', alignItems:'center', gap:8, flexShrink:0 }}>
        {!editing ? (
          <>
            <span style={{ fontSize:13.5, fontWeight:600, maxWidth:200, overflow:'hidden',
              textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
              {setting.settingValue !== null && setting.settingValue !== undefined
                ? (isBoolean
                    ? (setting.settingValue === 'true'
                        ? <span style={{ color:C.tertiary }}>✓ Enabled</span>
                        : <span style={{ color:C.onVariant }}>✗ Disabled</span>)
                    : setting.settingValue || '(empty)')
                : '(not set)'}
            </span>
            {!isReadOnly && (
              <button onClick={() => { setValue(setting.settingValue || ''); setEditing(true); }}
                style={{ display:'inline-flex', alignItems:'center', gap:4, padding:'6px 12px',
                  background:'#f0f3ff', border:`1px solid ${C.outline}`, borderRadius:7,
                  fontSize:12.5, fontWeight:600, cursor:'pointer', color:C.primary, fontFamily:'inherit', minHeight:32 }}>
                <span className="material-symbols-outlined" style={{ fontSize:14 }}>edit</span>
                Edit
              </button>
            )}
          </>
        ) : (
          <div className="as-edit-form" style={{ display:'flex', alignItems:'center', gap:8, flexWrap:'wrap', width:'100%' }}>
            {isBoolean ? (
              <select value={value} onChange={e => setValue(e.target.value)}
                style={{ padding:'7px 10px', border:`1px solid ${C.primary}`, borderRadius:7,
                  outline:'none', fontSize:13, fontFamily:'inherit', minWidth:120, flex:1 }}>
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
            ) : (
              <input value={value} onChange={e => setValue(e.target.value)}
                style={{ padding:'7px 12px', border:`1px solid ${C.primary}`, borderRadius:7,
                  outline:'none', fontSize:13, fontFamily:'inherit', minWidth:160, flex:1,
                  boxSizing:'border-box' }} />
            )}
            <div style={{ display:'flex', gap:6 }}>
              <button onClick={handleSave} disabled={saving}
                style={{ padding:'7px 14px', background:C.primary, color:'#fff', border:'none',
                  borderRadius:7, fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit', minHeight:32 }}>
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button onClick={() => setEditing(false)}
                style={{ padding:'7px 12px', background:'none', border:`1px solid ${C.outline}`,
                  borderRadius:7, fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit', minHeight:32 }}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function AdminSettings() {
  const { user } = useAuth();
  const addToast = useToast();

  const [settings, setSettings] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAllSettings();
      setSettings(data || {});
      if (!activeCategory && Object.keys(data || {}).length > 0) {
        setActiveCategory(Object.keys(data)[0]);
      }
    } catch {
      addToast('Failed to load settings', 'error');
    } finally { setLoading(false); }
  }, [activeCategory, addToast]);

  useEffect(() => { load(); }, [load]);

  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;

  const categories = Object.keys(settings);

  const handleSave = async (key, value) => {
    await updateSetting(key, value);
    await load();
  };

  if (loading) {
    return <div style={{ display:'flex', justifyContent:'center', padding:64 }}><LoadingSpinner size="lg" /></div>;
  }

  return (
    <div style={{ fontFamily:"'Manrope',sans-serif", color:C.onSurface, padding:0 }}>
      <style>{`
        .as-layout {
          display: flex;
          gap: 20px;
          align-items: flex-start;
        }
        .as-cat-sidebar {
          width: 210px;
          flex-shrink: 0;
          background: #ffffff;
          border-radius: 14px;
          border: 1px solid #e2e8f0;
          overflow: hidden;
          box-shadow: 0 1px 3px rgba(15,23,42,0.04);
        }
        .as-cat-btn {
          width: 100%;
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px 16px;
          border: none;
          cursor: pointer;
          text-align: left;
          font-family: inherit;
          font-size: 13.5px;
          font-weight: 600;
          background: transparent;
          color: ${C.onSurface};
          border-left: 3px solid transparent;
          transition: all 0.15s ease;
          white-space: nowrap;
        }
        .as-cat-btn.active {
          background: ${C.primaryBg};
          color: ${C.primary};
          border-left-color: ${C.primary};
        }

        @media (max-width: 768px) {
          .as-layout {
            flex-direction: column !important;
            gap: 14px !important;
          }
          .as-cat-sidebar {
            width: 100% !important;
            display: flex !important;
            flex-direction: row !important;
            overflow-x: auto !important;
            background: transparent !important;
            border: none !important;
            box-shadow: none !important;
            gap: 8px !important;
            padding: 2px 0 6px !important;
            -webkit-overflow-scrolling: touch !important;
            scrollbar-width: none !important;
          }
          .as-cat-sidebar::-webkit-scrollbar { display: none; }
          .as-cat-btn {
            width: auto !important;
            border-radius: 99px !important;
            border: 1px solid #e2e8f0 !important;
            background: #ffffff !important;
            padding: 8px 14px !important;
            font-size: 13px !important;
            border-left: 1px solid #e2e8f0 !important;
          }
          .as-cat-btn.active {
            background: ${C.primary} !important;
            color: #ffffff !important;
            border-color: ${C.primary} !important;
          }
          .as-cat-btn.active span.material-symbols-outlined {
            color: #ffffff !important;
          }
          .as-cat-btn.active span.badge {
            background: rgba(255,255,255,0.25) !important;
            color: #ffffff !important;
          }
          .as-setting-row {
            flex-direction: column !important;
            align-items: stretch !important;
            gap: 10px !important;
            padding: 16px 0 !important;
          }
          .as-setting-controls {
            width: 100% !important;
            justify-content: space-between !important;
          }
          .as-edit-form {
            width: 100% !important;
            flex-direction: column !important;
            align-items: stretch !important;
          }
        }
      `}</style>

      {/* Header */}
      <div style={{ marginBottom:20 }}>
        <h1 style={{ margin:0, fontSize:22, fontWeight:800, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Platform Settings</h1>
        <p style={{ margin:'4px 0 0', fontSize:13.5, color:C.onVariant }}>
          Configuration values stored in database · Managed by administrator
        </p>
        <div style={{
          display:'flex', alignItems:'flex-start', gap:8,
          marginTop:10, padding:'10px 14px',
          background:'rgba(0,88,190,0.06)', borderRadius:10,
          border:'1px solid rgba(0,88,190,0.15)'
        }}>
          <span className="material-symbols-outlined" style={{ fontSize:16, color:C.primary, flexShrink:0, marginTop:1 }}>info</span>
          <p style={{ margin:0, fontSize:12.5, color:C.onVariant, lineHeight:1.5 }}>
            Settings are persisted to the database and take effect dynamically across all active users.
          </p>
        </div>
      </div>

      <div className="as-layout">
        {/* Category Navigation (Sidebar on Desktop, Horizontal Pill Bar on Mobile/Tablet) */}
        <div className="as-cat-sidebar">
          {categories.map(cat => {
            const isActive = activeCategory === cat;
            return (
              <button key={cat} onClick={() => setActiveCategory(cat)}
                className={`as-cat-btn${isActive ? ' active' : ''}`}>
                <span className="material-symbols-outlined" style={{ fontSize:16,
                  color: isActive ? C.primary : C.onVariant,
                  fontVariationSettings: isActive ? "'FILL' 1" : "'FILL' 0" }}>
                  {CATEGORY_ICONS[cat] || 'settings'}
                </span>
                {cat}
                <span className="badge" style={{ marginLeft:'auto', fontSize:11, color: isActive ? C.primary : C.onVariant,
                  background: isActive ? '#dde8ff' : '#f0f3ff', padding:'2px 7px', borderRadius:99, fontWeight:700 }}>
                  {(settings[cat] || []).length}
                </span>
              </button>
            );
          })}
        </div>

        {/* Settings Panel */}
        <div style={{ flex:1, minWidth:0, background:'#ffffff', borderRadius:14, border:'1px solid #e2e8f0',
          padding:'18px 20px', boxShadow:'0 1px 3px rgba(15,23,42,0.04)' }}>
          {activeCategory && (
            <>
              <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:16,
                paddingBottom:14, borderBottom:`1px solid #f0f3ff` }}>
                <div style={{ width:34, height:34, borderRadius:8, background:C.primaryBg,
                  display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                  <span className="material-symbols-outlined" style={{ fontSize:18, color:C.primary,
                    fontVariationSettings:"'FILL' 1" }}>
                    {CATEGORY_ICONS[activeCategory] || 'settings'}
                  </span>
                </div>
                <div>
                  <h2 style={{ margin:0, fontSize:16, fontWeight:700 }}>{activeCategory} Settings</h2>
                  <p style={{ margin:0, fontSize:12, color:C.onVariant }}>
                    {(settings[activeCategory] || []).length} configuration keys
                  </p>
                </div>
              </div>

              {settings[activeCategory]?.map(s => (
                <SettingRow key={s.settingKey} setting={s} onSave={handleSave} />
              ))}

              {(!settings[activeCategory] || settings[activeCategory].length === 0) && (
                <p style={{ color:C.onVariant, textAlign:'center', padding:32 }}>
                  No settings in this category
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
