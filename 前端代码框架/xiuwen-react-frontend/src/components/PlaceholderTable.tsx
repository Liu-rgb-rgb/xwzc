import { Table } from 'antd';

interface Props { columns?: any[]; data?: any[]; loading?: boolean; }

export default function PlaceholderTable({ columns, data, loading }: Props) {
  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns || [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '状态', dataIndex: 'status' }
      ]}
      dataSource={data || []}
      pagination={{ pageSize: 10 }}
    />
  );
}
