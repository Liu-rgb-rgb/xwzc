import { Typography } from 'antd';

interface Props { title: string; desc?: string; }

export default function PageHeader({ title, desc }: Props) {
  return (
    <div style={{ marginBottom: 20 }}>
      <Typography.Title level={2} style={{ marginBottom: 4, color: '#5a351c' }}>{title}</Typography.Title>
      {desc && <Typography.Text type="secondary">{desc}</Typography.Text>}
    </div>
  );
}
