import { useEffect, useState } from 'react';
import { Button, Form, Input, Tabs } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getUserProfileApi, updateUserProfileApi, getAddressListApi, createAddressApi } from '@/api/user';
import { getMessagesApi } from '@/api/messages';
import PlaceholderTable from '@/components/PlaceholderTable';

export default function ProfilePage() {
  const [profile, setProfile] = useState<any>({});
  const [addresses, setAddresses] = useState<any[]>([]);
  const [messages, setMessages] = useState<any[]>([]);
  const load = () => {
    getUserProfileApi().then(setProfile).catch(() => {});
    getAddressListApi().then((res: any) => setAddresses(res?.list || res || [])).catch(() => {});
    getMessagesApi().then((res: any) => setMessages(res?.list || [])).catch(() => {});
  };
  useEffect(load, []);
  return (
    <div className="page-card">
      <PageHeader title="个人中心" desc="资料、地址、消息。" />
      <Tabs items={[
        { key: 'profile', label: '个人资料', children: <Form layout="vertical" initialValues={profile} onFinish={(v) => updateUserProfileApi(v).then(load)}><Form.Item label="昵称" name="nickname"><Input /></Form.Item><Form.Item label="邮箱" name="email"><Input /></Form.Item><Button type="primary" htmlType="submit">保存</Button></Form> },
        { key: 'address', label: '收货地址', children: <><Button onClick={() => createAddressApi({ receiverName: '张三', receiverPhone: '13800000000', detailAddress: '示例地址' }).then(load)}>新增示例地址</Button><PlaceholderTable data={addresses} /></> },
        { key: 'messages', label: '消息中心', children: <PlaceholderTable data={messages} /> }
      ]} />
    </div>
  );
}
