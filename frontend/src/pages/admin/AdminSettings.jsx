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
    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center',
      padding:'14px 0', borderBottom:`1px solid #f0f3ff`, gap:16 }}>
      <div style={{ flex:1, minWidth:0 }}>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          <code style={{ fontSize:12.5, fontFamily:'monospace', color:C.primary,
            background:C.primaryBg, padding:'2px 7px', borderRadius:5 }}>
            {setting.settingKey}
          </code>
          {isReadOnly && (
            <span style={{ fontSize:11, color:C.onVariant, background:'#f0f3ff',
              padding:'2px 7px', borderRadius:5, fontWeight:600 }}>Read-only</span>
          )}
          <span style={{ fontSize:11, color:C.onVariant, background:'#f0f3ff',
            padding:'2px 7px', borderRadius:5 }}>{setting.valueType}</span>
        </div>
        <p style={{ margin:'4px 0 0', fontSize:13, color:C.onVariant }}>{setting.description}</p>
        {setting.updatedBy && (
          <p style={{ margin:'2px 0 0', fontSize:11.5, color:'#9aa5b4' }}>
            Last updated by {setting.updatedBy}
          </p>
        )}
      </div>

      <div style={{ display:'flex', alignItems:'center', gap:8, flexShrink:0 }}>
        {!editing ? (
          <>
            <span style={{ fontSize:13.5, fontWeight:600, maxWidth:220, overflow:'hidden',
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
                style={{ display:'inline-flex', alignItems:'center', gap:4, padding:'5px 12px',
                  background:'#f0f3ff', border:`1px solid ${C.outline}`, borderRadius:7,
                  fontSize:12.5, fontWeight:600, cursor:'pointer', color:C.primary, fontFamily:'inherit' }}>
                <span className="material-symbols-outlined" style={{ fontSize:13 }}>edit</span>
                Edit
              </button>
            )}
          </>
        ) : (
          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
            {isBoolean ? (
              <select value={value} onChange={e => setValue(e.target.value)}
                style={{ padding:'6px 10px', border:`1px solid ${C.primary}`, borderRadius:7,
                  outline:'none', fontSize:13, fontFamily:'inherit', minWidth:120 }}>
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
            ) : (
              <input value={value} onChange={e => setValue(e.target.value)}
                style={{ padding:'7px 12px', border:`1px solid ${C.primary}`, borderRadius:7,
                  outline:'none', fontSize:13, fontFamily:'inherit', minWidth:220,
                  boxSizing:'border-box' }} />
            )}
            <button onClick={handleSave} disabled={saving}
              style={{ padding:'6px 14px', background:C.primary, color:'#fff', border:'none',
                borderRadius:7, fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit' }}>
              {saving ? 'Saving...' : 'Save'}
            </button>
            <button onClick={() => setEditing(false)}
              style={{ padding:'6px 12px', background:'none', border:`1px solid ${C.outline}`,
                borderRadius:7, fontWeight:600, fontSize:13, cursor:'pointer', fontFamily:'inherit' }}>
              Cancel
            </button>
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
  }, [addToast]);

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
    <div style={{ fontFamily:"'Manrope',sans-serif", color:C.onSurface }}>
      {/* Header */}
      <div style={{ marginBottom:24 }}>
        <h1 style={{ margin:0, fontSize:22, fontWeight:800, fontFamily:"'Plus Jakarta Sans',sans-serif" }}>Platform Settings</h1>
        <p style={{ margin:'4px 0 0', fontSize:13.5, color:C.onVariant }}>
          Configuration values stored in database · Some settings may require backend integration or a server restart to take effect
        </p>
        <div style={{
          display:'flex', alignItems:'flex-start', gap:8,
          marginTop:10, padding:'10px 14px',
          background:'rgba(0,88,190,0.06)', borderRadius:8,
          border:'1px solid rgba(0,88,190,0.15)'
        }}>
          <span className="material-symbols-outlined" style={{ fontSize:16, color:C.primary, flexShrink:0, marginTop:1 }}>info</span>
          <p style={{ margin:0, fontSize:12.5, color:C.onVariant, lineHeight:1.5 }}>
            AI and feature settings may be applied dynamically in future versions.
            Core security and infrastructure settings are currently managed through application configuration.
          </p>
        </div>
      </div>

      <div style={{ display:'flex', gap:20, alignItems:'flex-start' }}>
        {/* Category Sidebar */}
        <div style={{ width:200, flexShrink:0, background:'var(--snt-surface)', borderRadius:12,
          border:`1px solid ${C.outline}`, overflow:'hidden', position:'sticky', top:80 }}>
          {categories.map(cat => (
            <button key={cat} onClick={() => setActiveCategory(cat)}
              style={{ width:'100%', display:'flex', alignItems:'center', gap:10,
                padding:'12px 16px', border:'none', cursor:'pointer', textAlign:'left',
                fontFamily:'inherit', fontSize:13.5, fontWeight:600,
                background: activeCategory === cat ? C.primaryBg : 'transparent',
                color: activeCategory === cat ? C.primary : C.onSurface,
                borderLeft: activeCategory === cat ? `3px solid ${C.primary}` : '3px solid transparent',
                transition:'all 0.15s' }}>
              <span className="material-symbols-outlined" style={{ fontSize:16,
                fontVariationSettings: activeCategory === cat ? "'FILL' 1" : "'FILL' 0" }}>
                {CATEGORY_ICONS[cat] || 'settings'}
              </span>
              {cat}
              <span style={{ marginLeft:'auto', fontSize:11, color:C.onVariant,
                background:'#f0f3ff', padding:'2px 7px', borderRadius:99, fontWeight:700 }}>
                {(settings[cat] || []).length}
              </span>
            </button>
          ))}
        </div>

        {/* Settings Panel */}
        <div style={{ flex:1, background:'var(--snt-surface)', borderRadius:12, border:`1px solid ${C.outline}`,
          padding:'20px 24px', boxShadow:'0 2px 8px rgba(17,28,45,0.04)' }}>
          {activeCategory && (
            <>
              <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:20,
                paddingBottom:16, borderBottom:`1px solid #f0f3ff` }}>
                <div style={{ width:36, height:36, borderRadius:8, background:C.primaryBg,
                  display:'flex', alignItems:'center', justifyContent:'center' }}>
                  <span className="material-symbols-outlined" style={{ fontSize:18, color:C.primary,
                    fontVariationSettings:"'FILL' 1" }}>
                    {CATEGORY_ICONS[activeCategory] || 'settings'}
                  </span>
                </div>
                <div>
                  <h2 style={{ margin:0, fontSize:16, fontWeight:700 }}>{activeCategory}</h2>
                  <p style={{ margin:0, fontSize:12.5, color:C.onVariant }}>
                    {(settings[activeCategory] || []).length} configuration values
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
