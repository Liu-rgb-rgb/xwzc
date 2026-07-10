import { useEffect, useState } from 'react';
import { Button, Form, Input } from 'antd';
import PageHeader from '@/components/PageHeader';
import { getAdminShopInfoApi, updateAdminShopInfoApi } from '@/api/adminShop';

export default function AdminShopPage() {
  const [form] = Form.useForm();
  const [data, setData] = useState<any>({});
  useEffect(() => { getAdminShopInfoApi().then((res) => { setData(res); form.setFieldsValue(res); }).catch(() => {}); }, []);
  return <><PageHeader title="店铺设置" desc="接口对应：GET/PUT /api/admin/shop/info。" /><Form form={form} layout="vertical" initialValues={data} onFinish={(values) => updateAdminShopInfoApi(values)}><Form.Item label="店铺名称" name="shopName"><Input /></Form.Item><Form.Item label="联系方式" name="contactPhone"><Input /></Form.Item><Form.Item label="地址" name="address"><Input /></Form.Item><Form.Item label="介绍" name="description"><Input.TextArea rows={4} /></Form.Item><Button type="primary" htmlType="submit">保存</Button></Form></>;
}
