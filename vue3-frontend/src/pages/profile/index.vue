<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, listFrom, type Id } from '../../api';
import { logout } from '../../auth';
import { readUserData, writeUserData } from '../../userData';

type Gender = 'UNKNOWN' | 'MALE' | 'FEMALE';
type MenuKey = 'profile' | 'patterns' | 'favorites' | 'orders' | 'customize' | 'addresses' | 'messages' | 'security' | 'settings';

interface ProfileForm {
  id?: number;
  username?: string;
  nickname: string;
  phone: string;
  email: string;
  gender: Gender;
  birthday: string;
  intro: string;
  avatar: string;
  preferredStyle: string;
  preferredCategory: string;
  lastLoginAt?: string;
}

interface AddressForm {
  receiverName: string;
  receiverPhone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  isDefault: number;
}

interface Address extends AddressForm { id: Id; }
interface Message { id: Id; title?: string; content?: string; isRead?: number; createdAt?: string; }

const router = useRouter();
const route = useRoute();
const activeMenu = ref<MenuKey>('profile');
const loading = ref(false);
const saving = ref(false);
const notice = ref('');
const error = ref('');
const avatarInput = ref<HTMLInputElement | null>(null);
const phoneInput = ref<HTMLInputElement | null>(null);
const profile = ref<ProfileForm>({
  nickname: '绣纹爱好者', phone: '', email: '', gender: 'UNKNOWN', birthday: '', intro: '', avatar: '',
  preferredStyle: '传统吉祥纹样', preferredCategory: '家居装饰'
});
const stats = ref({ patterns: 0, favorites: 0, orders: 0 });
const addresses = ref<Address[]>([]);
const messages = ref<Message[]>([]);
const customDesigns = ref<any[]>([]);
const orders = ref<any[]>([]);
const unreadCount = ref(0);
const editingAddressId = ref<Id | null>(null);
const addressForm = ref<AddressForm>({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 });
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });
const passwordEditing = ref(false);
const passwordPopup = ref({ visible: false, success: false, title: '', message: '' });

const menus: Array<{ key: MenuKey; label: string; icon: string; path?: string }> = [
  { key: 'profile', label: '个人资料', icon: '♙' },
  { key: 'patterns', label: '我的纹样', icon: '♧', path: '/patterns' },
  { key: 'favorites', label: '我的收藏', icon: '♡', path: '/patterns?tab=favorite' },
  { key: 'orders', label: '我的订单', icon: '▣' },
  { key: 'customize', label: '我的定制', icon: '⌘' },
  { key: 'addresses', label: '收货地址', icon: '⌖' },
  { key: 'messages', label: '消息中心', icon: '◌' },
  { key: 'security', label: '账号安全', icon: '◇' },
  { key: 'settings', label: '设置', icon: '⚙' }
];

const defaultAddress = computed(() => addresses.value.find((item) => Number(item.isDefault) === 1) || addresses.value[0]);
const currentMenu = computed(() => menus.find((item) => item.key === activeMenu.value)?.label || '个人资料');

function normalizeProfile(value: any): ProfileForm {
  const local = readUserData<any>('profile', {});
  return {
    ...profile.value,
    ...local,
    ...(value || {}),
    preferredStyle: value?.preferredStyle || local?.preferredStyle || profile.value.preferredStyle,
    preferredCategory: value?.preferredCategory || local?.preferredCategory || profile.value.preferredCategory
  };
}

function totalFrom(result: any): number {
  const value = result?.total ?? result?.totalCount ?? result?.pagination?.total;
  return Number(value ?? listFrom(result).length ?? 0);
}
function orderItems(order: any) {
  const value = order?.items || order?.orderItems || order?.products || [];
  return Array.isArray(value) ? value : [];
}
function orderAmount(order: any) {
  const amount = order?.totalAmount ?? order?.payAmount ?? order?.amount;
  if (amount != null) return Number(amount || 0);
  return orderItems(order).reduce((sum: number, item: any) =>
    sum + Number(item.price || item.productPrice || 0) * Number(item.quantity || 1), 0);
}

async function loadPage() {
  loading.value = true;
  error.value = '';
  const [userResult, statsResult, addressResult, messageResult, unreadResult, customResult, orderResult] = await Promise.allSettled([
    api.auth.me(), api.user.getStats(), api.user.addresses(), api.messages.list({ page: 1, pageSize: 20 }), api.messages.unreadCount(), api.customDesigns.mine({ page: 1, pageSize: 20 }), api.orders.mine({ page: 1, pageSize: 50 })
  ]);
  if (userResult.status === 'fulfilled') profile.value = normalizeProfile(userResult.value);
  else profile.value = normalizeProfile(null);
  if (statsResult.status === 'fulfilled') stats.value = { ...stats.value, ...statsResult.value };
  const localPatterns = [
    ...readUserData<any[]>('saved_patterns', []),
    ...readUserData<any[]>('recent_generations', [])
  ].filter((item, index, values) => values.findIndex((other) => String(other.id) === String(item.id)) === index);
  stats.value.patterns = Math.max(stats.value.patterns, localPatterns.length);
  stats.value.favorites = Math.max(stats.value.favorites, readUserData<string[]>('pattern_favorites', []).length);
  if (addressResult.status === 'fulfilled') addresses.value = listFrom(addressResult.value) as Address[];
  if (messageResult.status === 'fulfilled') messages.value = listFrom(messageResult.value) as Message[];
  if (unreadResult.status === 'fulfilled') unreadCount.value = Number(unreadResult.value || 0);
  const localDesigns = readUserData<any[]>('applied_designs', []);
  const remoteDesigns = customResult.status === 'fulfilled' ? listFrom(customResult.value) : [];
  customDesigns.value = [...remoteDesigns, ...localDesigns].filter((design, index, values) =>
    values.findIndex((other) => String(other.id) === String(design.id)) === index
  );
  const localOrders = readUserData<any[]>('user_orders', []);
  const remoteOrders = orderResult.status === 'fulfilled' ? listFrom(orderResult.value) : [];
  orders.value = [...remoteOrders, ...localOrders].filter((order, index, values) =>
    values.findIndex((other) => String(other.id || other.orderNo) === String(order.id || order.orderNo)) === index
  );
  stats.value.orders = Math.max(stats.value.orders, orders.value.length);
  if (userResult.status === 'rejected' && addressResult.status === 'rejected') error.value = '接口暂时不可用，当前显示本地资料。';
  loading.value = false;
}

function selectMenu(menu: (typeof menus)[number]) {
  activeMenu.value = menu.key;
  if (menu.path) router.push(menu.path);
}

async function saveProfile() {
  saving.value = true;
  notice.value = '';
  error.value = '';
  const payload = {
    nickname: profile.value.nickname,
    phone: profile.value.phone,
    email: profile.value.email,
    gender: profile.value.gender,
    birthday: profile.value.birthday,
    intro: profile.value.intro
  };
  try {
    const result = await api.auth.updateProfile(payload);
    profile.value = normalizeProfile(result);
    writeUserData('profile', profile.value);
    notice.value = '资料保存成功';
  } catch {
    writeUserData('profile', profile.value);
    notice.value = '后端暂不可用，资料已保存到本地';
  } finally { saving.value = false; }
}

async function chooseAvatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    error.value = '头像仅支持 5MB 以内的 JPG 或 PNG';
    return;
  }
  const data = new FormData(); data.append('file', file);
  try {
    const result: any = await api.user.uploadAvatar(data);
    profile.value.avatar = result?.avatar || result?.fileUrl || URL.createObjectURL(file);
    writeUserData('profile', profile.value);
    notice.value = '头像上传成功';
  } catch {
    profile.value.avatar = URL.createObjectURL(file);
    writeUserData('profile', profile.value);
    notice.value = '后端暂不可用，已显示本地头像';
  } finally { (event.target as HTMLInputElement).value = ''; }
}

function editDefaultAddress() {
  activeMenu.value = 'addresses';
  if (defaultAddress.value) {
    editingAddressId.value = defaultAddress.value.id;
    addressForm.value = { ...defaultAddress.value };
  }
}

function resetAddressForm() {
  editingAddressId.value = null;
  addressForm.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 };
}

async function saveAddress() {
  const payload = { ...addressForm.value };
  try {
    const result: any = editingAddressId.value
      ? await api.user.updateAddress(editingAddressId.value, payload)
      : await api.user.createAddress(payload);
    const saved = { ...payload, ...(result || {}), id: result?.id || editingAddressId.value || Date.now() } as Address;
    addresses.value = editingAddressId.value
      ? addresses.value.map((item) => item.id === editingAddressId.value ? saved : item)
      : [...addresses.value, saved];
    notice.value = '收货地址已保存';
  } catch {
    const saved = { ...payload, id: editingAddressId.value || Date.now() } as Address;
    addresses.value = editingAddressId.value
      ? addresses.value.map((item) => item.id === editingAddressId.value ? saved : item)
      : [...addresses.value, saved];
    notice.value = '后端暂不可用，地址已保存到本地';
  }
  resetAddressForm();
}

async function removeAddress(address: Address) {
  try { await api.user.deleteAddress(address.id); } catch { /* 本地模式继续删除 */ }
  addresses.value = addresses.value.filter((item) => item.id !== address.id);
  notice.value = '收货地址已删除';
}

async function makeDefault(address: Address) {
  try { await api.user.setDefaultAddress(address.id); } catch { /* 本地模式继续设置 */ }
  addresses.value = addresses.value.map((item) => ({ ...item, isDefault: item.id === address.id ? 1 : 0 }));
  notice.value = '默认地址已更新';
}

async function markMessageRead(message: Message) {
  if (Number(message.isRead) === 1) return;
  try { await api.messages.markRead(message.id); } catch { /* 本地模式继续更新 */ }
  message.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1);
}

async function removeMessage(message: Message) {
  try { await api.messages.remove(message.id); } catch { /* 本地模式继续删除 */ }
  messages.value = messages.value.filter((item) => item.id !== message.id);
  if (Number(message.isRead) !== 1) unreadCount.value = Math.max(0, unreadCount.value - 1);
}

async function removeCustomDesign(design: any) {
  try { await api.customDesigns.remove(design.id); } catch { /* 本地模式继续移除 */ }
  customDesigns.value = customDesigns.value.filter((item) => item.id !== design.id);
  writeUserData('applied_designs', readUserData<any[]>('applied_designs', []).filter((item) => item.id !== design.id));
  notice.value = '定制商品已移除';
}

async function updatePassword() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordPopup.value = { visible: true, success: false, title: '密码修改失败', message: '两次输入的新密码不一致，请重新输入。' };
    return;
  }
  try {
    await api.user.updatePassword(passwordForm.value);
    passwordPopup.value = { visible: true, success: true, title: '密码修改成功', message: '新密码已经生效，请妥善保管。' };
    cancelPasswordEdit();
  } catch (reason: any) {
    passwordPopup.value = {
      visible: true,
      success: false,
      title: '密码修改失败',
      message: reason?.response?.data?.message || '请检查当前密码是否正确，或稍后重试。'
    };
  }
}

function openPasswordEdit() {
  activeMenu.value = 'security';
  passwordEditing.value = true;
  notice.value = '';
}

async function openPhoneBinding() {
  activeMenu.value = 'profile';
  await nextTick();
  phoneInput.value?.focus();
}

function cancelPasswordEdit() {
  passwordEditing.value = false;
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
}

function closePasswordPopup() {
  passwordPopup.value.visible = false;
}

function signOut() {
  void api.auth.logout().catch(() => {});
  logout();
  router.push('/login');
}

onMounted(() => {
  const section = String(route.query.section || '');
  if (menus.some((menu) => menu.key === section)) activeMenu.value = section as MenuKey;
  loadPage();
});
</script>

<template>
  <!-- 页面整体：国风浅色个人中心布局 -->
  <section class="profile-page">
    <div class="profile-heading">
      <div><span class="eyebrow">XIUWEN PERSONAL CENTER</span><h1>个人中心</h1><p>管理您的账号信息与偏好设置</p></div>
      <span v-if="loading" class="loading-text">正在同步资料…</span>
    </div>

    <div class="profile-layout">
      <!-- 左侧侧边导航 -->
      <aside class="profile-sidebar">
        <button v-for="menu in menus" :key="menu.key" class="side-item" :class="{ active: activeMenu === menu.key }" @click="selectMenu(menu)">
          <span class="side-icon">{{ menu.icon }}</span><span>{{ menu.label }}</span>
          <em v-if="menu.key === 'messages' && unreadCount">{{ unreadCount }}</em>
        </button>
      </aside>

      <main class="profile-main">
        <!-- 顶部统计卡片 -->
        <div class="stats-grid">
          <article class="stat-card jade"><span class="stat-icon">♧</span><div><b>{{ stats.patterns }}</b><small>件作品</small><strong>我的纹样</strong></div><button @click="router.push('/patterns')">查看全部 ›</button></article>
          <article class="stat-card red"><span class="stat-icon">♥</span><div><b>{{ stats.favorites }}</b><small>个收藏</small><strong>我的收藏</strong></div><button @click="router.push('/patterns?tab=favorite')">查看全部 ›</button></article>
          <article class="stat-card gold"><span class="stat-icon">▣</span><div><b>{{ stats.orders }}</b><small>个订单</small><strong>我的订单</strong></div><button @click="activeMenu = 'orders'">查看全部 ›</button></article>
        </div>

        <p v-if="error" class="form-message error">{{ error }}</p><p v-if="notice" class="form-message">{{ notice }}</p>

        <!-- 个人资料主模块 -->
        <section v-if="activeMenu === 'profile' || activeMenu === 'security' || activeMenu === 'settings'" class="content-card">
          <div class="card-title"><div><span class="eyebrow">PROFILE</span><h2>{{ currentMenu === '个人资料' ? '完善头像与个人资料' : currentMenu }}</h2></div><span class="last-login">上次登录：{{ profile.lastLoginAt || '暂无记录' }}</span></div>
          <div class="profile-content-grid">
            <!-- 头像上传区域 -->
            <div class="avatar-block">
              <div class="avatar-frame"><img v-if="profile.avatar" :src="profile.avatar" alt="用户头像" /><span v-else>绣</span></div>
              <input ref="avatarInput" type="file" accept="image/jpeg,image/png" hidden @change="chooseAvatar" />
              <button class="upload-button" @click="avatarInput?.click()">⇧ 上传/更换头像</button><small>支持 JPG / PNG，建议尺寸 200×200px</small>
            </div>

            <!-- 资料表单：字段与 /api/user/profile 对齐 -->
            <form class="profile-form" @submit.prevent="saveProfile">
              <label>昵称 <input v-model="profile.nickname" maxlength="20" placeholder="请输入昵称" /></label>
              <label>手机号 <input ref="phoneInput" v-model="profile.phone" placeholder="请输入手机号" /></label>
              <label>邮箱 <input v-model="profile.email" type="email" placeholder="请输入邮箱" /></label>
              <fieldset><legend>性别</legend><label><input v-model="profile.gender" type="radio" value="FEMALE" /> 女</label><label><input v-model="profile.gender" type="radio" value="MALE" /> 男</label><label><input v-model="profile.gender" type="radio" value="UNKNOWN" /> 保密</label></fieldset>
              <label>生日 <input v-model="profile.birthday" type="date" /></label>
              <label>偏好纹样风格 <select v-model="profile.preferredStyle"><option>传统吉祥纹样</option><option>广绣经典</option><option>新中式</option><option>岭南花窗</option></select></label>
              <label>偏好商品类别 <select v-model="profile.preferredCategory"><option>家居装饰</option><option>帆布袋</option><option>丝巾</option><option>笔记本</option></select></label>
              <label class="full-width">个人简介 <textarea v-model="profile.intro" maxlength="200" placeholder="介绍一下您自己"></textarea><small>{{ profile.intro.length }}/200</small></label>
              <button class="primary save-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存资料' }}</button>
            </form>

            <!-- 账号安全模块 -->
            <aside class="security-card">
              <h3>◇ 账号安全</h3>
              <div class="security-row"><span>修改密码</span><button @click="openPasswordEdit">›</button></div>
              <form v-if="passwordEditing" class="password-form" @submit.prevent="updatePassword">
                <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password" placeholder="当前密码" required />
                <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" placeholder="新密码" required />
                <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" placeholder="确认新密码" required />
                <div class="password-actions"><button class="small primary" type="submit">确认修改</button><button class="small cancel-button" type="button" @click="cancelPasswordEdit">取消修改</button></div>
              </form>
              <div class="security-row"><span>绑定手机号<small>{{ profile.phone ? '已绑定' : '未绑定' }}</small></span><button @click="openPhoneBinding">›</button></div>
              <div class="security-row"><span>账号安全<small>{{ profile.lastLoginAt ? `上次登录 ${profile.lastLoginAt}` : '建议定期修改密码' }}</small></span><button @click="openPasswordEdit">›</button></div>
            </aside>
          </div>
          <div v-if="defaultAddress" class="default-address"><div><b>默认收货地址</b><p>⌖ {{ defaultAddress.province }}{{ defaultAddress.city }}{{ defaultAddress.district }}{{ defaultAddress.detailAddress }}</p><small>{{ defaultAddress.receiverName }} · {{ defaultAddress.receiverPhone }}</small></div><button @click="editDefaultAddress">编辑</button></div>
          <div v-else class="default-address empty-address"><span>暂未设置收货地址</span><button @click="activeMenu = 'addresses'">添加地址</button></div>
          <div v-if="activeMenu === 'settings'" class="settings-logout">
            <div><b>退出当前账号</b><p>退出后，再次使用购物车、订单和个人资料时需要重新登录。</p></div>
            <button class="logout-account" type="button" @click="signOut">退出账号</button>
          </div>
        </section>

        <!-- 我的订单模块 -->
        <section v-else-if="activeMenu === 'orders'" class="content-card">
          <div class="card-title"><div><span class="eyebrow">MY ORDERS</span><h2>我的订单</h2></div><span>{{ orders.length }} 个订单</span></div>
          <div class="profile-order-list">
            <article v-for="order in orders" :key="order.id || order.orderNo" class="profile-order-card">
              <div class="order-head"><div><b>订单号：{{ order.orderNo || order.id }}</b><small>{{ order.createdAt || order.createTime || '' }}</small></div><i>{{ order.statusName || order.status || '待付款' }}</i></div>
              <div class="order-products">
                <div v-for="item in orderItems(order)" :key="item.id || item.productId">
                  <img :src="item.productImage || item.image || '/demo/product/peony-canvas-bag-cover.jpg'" :alt="item.productName || item.name" />
                  <span><b>{{ item.productName || item.name || '文创商品' }}</b><small>¥ {{ Number(item.price || item.productPrice || 0).toFixed(2) }} × {{ item.quantity || 1 }}</small></span>
                </div>
              </div>
              <div class="order-total">共 {{ orderItems(order).reduce((sum, item) => sum + Number(item.quantity || 1), 0) }} 件商品 <strong>合计：¥ {{ orderAmount(order).toFixed(2) }}</strong></div>
            </article>
            <p v-if="!orders.length" class="empty-state">暂无订单，购物车提交后会显示在这里。</p>
          </div>
        </section>

        <!-- 我的定制商品模块 -->
        <section v-else-if="activeMenu === 'customize'" class="content-card"><div class="card-title"><div><span class="eyebrow">CUSTOM DESIGNS</span><h2>我的定制商品</h2></div><span>{{ customDesigns.length }} 件</span></div><div class="custom-design-list"><article v-for="design in customDesigns" :key="design.id" class="custom-design-item"><img :src="design.previewImageUrl || design.previewImage || design.productImage || design.image || '/demo/product/peony-canvas-bag-cover.jpg'" alt="定制商品预览" /><div><h3>{{ design.productName || design.product?.name || design.title || `定制商品 #${design.id}` }}</h3><p>{{ design.remark || design.description || '专属广绣纹样定制作品' }}</p><small>{{ design.createdAt || '' }}</small><div><button v-if="design.productId" @click="router.push(`/products/${design.productId}`)">查看商品</button><button @click="removeCustomDesign(design)">删除记录</button></div></div></article><p v-if="!customDesigns.length" class="empty-state">暂无定制商品，先去创建专属定制吧。</p></div></section>

        <!-- 收货地址模块 -->
        <section v-else-if="activeMenu === 'addresses'" class="content-card"><div class="card-title"><h2>收货地址</h2></div><form class="address-form" @submit.prevent="saveAddress"><input v-model="addressForm.receiverName" required placeholder="收货人" /><input v-model="addressForm.receiverPhone" required placeholder="联系电话" /><input v-model="addressForm.province" placeholder="省" /><input v-model="addressForm.city" placeholder="市" /><input v-model="addressForm.district" placeholder="区/县" /><input v-model="addressForm.detailAddress" required class="address-detail" placeholder="详细地址" /><label><input v-model="addressForm.isDefault" type="checkbox" :true-value="1" :false-value="0" /> 设为默认</label><button class="primary small" type="submit">{{ editingAddressId ? '保存修改' : '添加地址' }}</button></form><div class="address-list"><article v-for="address in addresses" :key="address.id" class="address-item"><div><b>{{ address.receiverName }}</b><span>{{ address.receiverPhone }}</span><i v-if="Number(address.isDefault) === 1">默认</i><p>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</p></div><div><button @click="editingAddressId = address.id; addressForm = { ...address }">修改</button><button @click="makeDefault(address)" :disabled="Number(address.isDefault) === 1">设为默认</button><button @click="removeAddress(address)">删除</button></div></article><p v-if="!addresses.length" class="empty-state">暂无收货地址</p></div></section>

        <!-- 消息中心模块 -->
        <section v-else-if="activeMenu === 'messages'" class="content-card"><div class="card-title"><div><h2>消息中心</h2><small>未读消息 {{ unreadCount }} 条</small></div><button class="small" @click="messages.forEach(markMessageRead); unreadCount = 0">全部已读</button></div><div class="message-list"><article v-for="message in messages" :key="message.id" :class="{ unread: Number(message.isRead) !== 1 }" @click="markMessageRead(message)"><div><b>{{ message.title || '系统通知' }}</b><p>{{ message.content }}</p><small>{{ message.createdAt || '' }}</small></div><button @click.stop="removeMessage(message)">删除</button></article><p v-if="!messages.length" class="empty-state">暂无消息</p></div></section>
      </main>
    </div>
    <!-- 密码修改结果弹窗 -->
    <div v-if="passwordPopup.visible" class="modal-backdrop" role="presentation" @click.self="closePasswordPopup">
      <section class="result-dialog" role="alertdialog" aria-modal="true" :aria-label="passwordPopup.title">
        <span class="dialog-icon" :class="{ success: passwordPopup.success }">{{ passwordPopup.success ? '✓' : '!' }}</span>
        <h3>{{ passwordPopup.title }}</h3><p>{{ passwordPopup.message }}</p>
        <button class="primary" type="button" @click="closePasswordPopup">我知道了</button>
      </section>
    </div>
  </section>
</template>

<style scoped>
/* 页面基础：国风浅色、米白纸张与朱砂红点缀 */
.profile-page { min-height: 720px; padding: 42px max(5vw, 24px) 80px; background: linear-gradient(180deg, #fbf1e1, #fffaf3 40%); }
.profile-heading { max-width: 1380px; margin: 0 auto 28px; display: flex; justify-content: space-between; align-items: end; }
.profile-heading h1 { margin: 8px 0; font-size: clamp(34px, 4vw, 48px); letter-spacing: 4px; }
.profile-heading p, .loading-text, .last-login { color: var(--muted); }
.eyebrow { color: var(--red); font-size: 11px; letter-spacing: 3px; }
.profile-layout { max-width: 1380px; margin: auto; display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 24px; align-items: start; }
/* 左侧导航 */
.profile-sidebar { padding: 16px 10px; background: rgba(255,255,255,.78); border: 1px solid var(--line); border-radius: 16px; box-shadow: var(--shadow); }
.side-item { width: 100%; display: flex; align-items: center; gap: 12px; margin: 3px 0; padding: 13px 14px; border: 0; background: transparent; text-align: left; color: var(--ink); }
.side-item.active { color: var(--red); background: #fff0e8; border-radius: 10px; }
.side-icon { width: 22px; text-align: center; font-size: 20px; }.side-item em { margin-left: auto; min-width: 20px; border-radius: 20px; background: var(--red); color: #fff; font-size: 11px; text-align: center; }
.profile-main { min-width: 0; }.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 18px; }
/* 顶部统计卡片 */
.stat-card { display: flex; align-items: center; gap: 12px; min-height: 116px; padding: 18px; background: rgba(255,255,255,.85); border: 1px solid var(--line); border-radius: 14px; box-shadow: 0 8px 26px rgba(87,55,27,.08); }.stat-icon { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 50%; color: #fff; font-size: 26px; }.stat-card.jade .stat-icon { background: #77b7aa; }.stat-card.red .stat-icon { background: #d96753; }.stat-card.gold .stat-icon { background: #c79c58; }.stat-card div { display: flex; align-items: baseline; gap: 7px; flex-wrap: wrap; }.stat-card strong { order: -1; flex-basis: 100%; }.stat-card b { color: var(--jade); font-size: 30px; font-weight: 500; }.stat-card.red b { color: var(--red); }.stat-card small { color: var(--muted); }.stat-card button { margin-left: auto; padding: 3px 0; border: 0; background: transparent; color: var(--jade); font-size: 12px; white-space: nowrap; }
.content-card { padding: 24px; background: rgba(255,255,255,.84); border: 1px solid var(--line); border-radius: 16px; box-shadow: var(--shadow); }.card-title { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid var(--line); }.card-title h2 { margin: 5px 0 0; }.profile-content-grid { display: grid; grid-template-columns: 160px minmax(0, 1fr) 230px; gap: 24px; padding-top: 22px; }
/* 头像区域 */
.avatar-block { text-align: center; }.avatar-frame { width: 132px; height: 132px; margin: 0 auto 14px; display: grid; place-items: center; overflow: hidden; border: 1px solid #e3cfb5; border-radius: 50%; background: #f0eee9; color: var(--red); font-size: 44px; }.avatar-frame img { width: 100%; height: 100%; object-fit: cover; }.upload-button { width: 100%; padding: 9px 6px; border: 0; border-radius: 20px; background: var(--red2); color: #fff; font-size: 13px; }.avatar-block small { display: block; margin-top: 9px; color: var(--muted); font-size: 11px; line-height: 1.5; }
/* 资料表单 */
.profile-form { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.profile-form label, .profile-form fieldset { display: flex; flex-direction: column; gap: 7px; border: 0; padding: 0; margin: 0; font-size: 13px; }.profile-form input, .profile-form select, .profile-form textarea, .address-form input { width: 100%; padding: 10px 11px; border: 1px solid var(--line); border-radius: 8px; background: #fffdfa; }.profile-form fieldset { flex-direction: row; align-items: center; gap: 14px; }.profile-form legend { float: left; width: 100%; margin-bottom: 3px; }.profile-form fieldset label { flex-direction: row; align-items: center; }.full-width { grid-column: 1 / -1; }.profile-form textarea { min-height: 86px; resize: vertical; }.profile-form small { align-self: end; color: var(--muted); font-size: 11px; }.save-button { grid-column: 1 / -1; justify-self: center; min-width: 180px; }
/* 账号安全 */
.security-card { min-width: 0; max-width: 100%; overflow: hidden; padding-left: 18px; border-left: 1px solid var(--line); }.security-card h3 { margin-top: 0; }.security-row { display: flex; align-items: center; justify-content: space-between; min-height: 62px; border-bottom: 1px solid var(--line); }.security-row small { display: block; margin-top: 4px; color: var(--muted); font-size: 11px; }.security-row button { border: 0; background: transparent; font-size: 22px; }.password-form { width: 100%; min-width: 0; display: grid; gap: 8px; padding: 10px 0; }.password-form input { width: 100%; min-width: 0; display: block; padding: 8px; border: 1px solid var(--line); border-radius: 7px; background: #fffdfa; }.password-actions { min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 8px; }.password-actions button { width: 100%; min-width: 0; padding: 8px 4px; font-size: 12px; }.cancel-button { color: var(--muted); background: #fffdfa; }
.default-address { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 22px; padding: 15px 17px; border: 1px solid var(--line); border-radius: 10px; background: #fffaf3; }.default-address p { margin: 8px 0 4px; }.default-address small { color: var(--muted); }.default-address button, .address-item button, .message-list button { border: 0; background: transparent; color: var(--red); }.empty-address { color: var(--muted); }
.settings-logout { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-top: 28px; padding: 20px; border-top: 1px solid var(--line); background: #fffaf3; border-radius: 0 0 12px 12px; }.settings-logout p { margin: 7px 0 0; color: var(--muted); font-size: 13px; line-height: 1.6; }.logout-account { flex: 0 0 auto; padding: 10px 20px; border: 1px solid var(--red); border-radius: 9px; background: #fff; color: var(--red); cursor: pointer; }
.form-message { margin: 12px 0; color: var(--jade); }.form-message.error { color: var(--red); }.small { padding: 8px 13px; }
/* 密码修改结果弹窗 */
.modal-backdrop { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: rgba(45, 33, 24, .45); backdrop-filter: blur(4px); }.result-dialog { width: min(390px, 100%); padding: 30px; border: 1px solid var(--line); border-radius: 16px; background: #fffaf3; box-shadow: 0 24px 70px rgba(45, 33, 24, .28); text-align: center; }.dialog-icon { width: 52px; height: 52px; margin: 0 auto 12px; display: grid; place-items: center; border-radius: 50%; background: #fff0e8; color: var(--red); font-size: 28px; font-weight: 700; }.dialog-icon.success { background: #e7f2eb; color: var(--jade); }.result-dialog h3 { margin: 8px 0; font-size: 23px; }.result-dialog p { margin: 0 0 22px; color: var(--muted); line-height: 1.7; }.result-dialog button { min-width: 130px; }
/* 地址与消息管理 */
.address-form { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; padding: 20px 0; }.address-detail { grid-column: span 2; }.address-form label { display: flex; align-items: center; gap: 6px; font-size: 13px; }.address-form .primary { justify-self: end; }.address-list, .message-list { display: grid; gap: 10px; }.address-item, .message-list article { display: flex; justify-content: space-between; gap: 18px; padding: 15px; border: 1px solid var(--line); border-radius: 10px; background: #fffdfa; }.address-item span { margin-left: 12px; color: var(--muted); }.address-item i { margin-left: 10px; padding: 2px 7px; border-radius: 10px; background: #fff0e8; color: var(--red); font-size: 11px; font-style: normal; }.address-item p { margin: 8px 0 0; color: var(--muted); }.message-list article { cursor: pointer; }.message-list article.unread { border-left: 3px solid var(--red); }.message-list article p { margin: 7px 0; color: var(--muted); }.message-list article small { color: #a39482; }.empty-state { padding: 28px; text-align: center; color: var(--muted); }
@media (max-width: 1100px) { .profile-content-grid { grid-template-columns: 140px minmax(0, 1fr); }.security-card { grid-column: 1 / -1; padding: 18px 0 0; border-left: 0; border-top: 1px solid var(--line); }.profile-layout { grid-template-columns: 180px minmax(0, 1fr); } }
@media (max-width: 760px) { .profile-page { padding: 28px 16px 55px; }.profile-layout { grid-template-columns: 1fr; }.profile-sidebar { display: grid; grid-template-columns: repeat(3, 1fr); gap: 3px; }.side-item { flex-direction: column; gap: 4px; padding: 9px 4px; text-align: center; font-size: 12px; }.side-item em { position: absolute; }.stats-grid { grid-template-columns: 1fr; }.profile-content-grid { grid-template-columns: 1fr; }.avatar-block { max-width: 180px; margin: auto; }.profile-form, .address-form { grid-template-columns: 1fr; }.full-width, .address-detail { grid-column: auto; }.address-form .primary { justify-self: start; }.address-item, .message-list article, .settings-logout { flex-direction: column; align-items: stretch; }.logout-account { width: 100%; } }
.custom-design-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; padding-top: 20px; }
.custom-design-item { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 14px; padding: 14px; border: 1px solid var(--line); border-radius: 10px; background: #fffdfa; }
.custom-design-item img { width: 120px; height: 120px; border-radius: 8px; object-fit: cover; }
.custom-design-item h3 { margin: 4px 0 8px; font-size: 17px; }.custom-design-item p { margin: 0 0 5px; color: var(--muted); font-size: 13px; }.custom-design-item small { color: #a39482; }.custom-design-item div > div { display: flex; gap: 10px; margin-top: 12px; }.custom-design-item button { padding: 5px 0; border: 0; background: transparent; color: var(--red); font-size: 12px; }
.profile-order-list { display: grid; gap: 14px; padding-top: 20px; }
.profile-order-card { overflow: hidden; border: 1px solid var(--line); border-radius: 11px; background: #fffdfa; }
.order-head, .order-total { display: flex; justify-content: space-between; align-items: center; gap: 14px; padding: 13px 16px; background: #fff8ed; }
.order-head div { display: grid; gap: 5px; }.order-head small { color: var(--muted); }.order-head i { color: var(--red); font-style: normal; }
.order-products { display: grid; gap: 10px; padding: 14px 16px; }.order-products > div { display: flex; align-items: center; gap: 12px; }.order-products img { width: 58px; height: 58px; object-fit: cover; border-radius: 7px; }.order-products span { display: grid; gap: 5px; }.order-products small { color: var(--muted); }
.order-total { justify-content: flex-end; color: var(--muted); border-top: 1px solid var(--line); }.order-total strong { color: var(--red); font-size: 18px; }
@media (max-width: 760px) { .custom-design-list { grid-template-columns: 1fr; }.custom-design-item { grid-template-columns: 92px minmax(0, 1fr); }.custom-design-item img { width: 92px; height: 92px; } }
</style>
