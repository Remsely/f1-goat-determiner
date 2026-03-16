# TypeScript / React Coding Style (frontend)

## Components

Functional components only. Never use class components.
Each component lives in its own file. File name matches the component name (`PascalCase`).

```tsx
// Good — DriverCard.tsx
interface DriverCardProps {
  driverId: string;
  name: string;
  tier: number;
}

export function DriverCard({ driverId, name, tier }: DriverCardProps) {
  return <div>...</div>;
}
```

Props interfaces: always explicit, named `<ComponentName>Props`.
Never use `React.FC` — type the props parameter directly.

## Exports

Prefer named exports over default exports. One component per file.

```typescript
// Good
export function TierList() { ... }

// Bad
export default function TierList() { ... }
```

## Types

No `any`. Use `unknown` when the type is truly unknown, then narrow it.
Prefer `interface` for object shapes, `type` for unions and aliases.

```typescript
// Good
interface ApiResponse<T> {
  data: T;
  status: number;
}

// Bad
const response: any = await api.get('/drivers');
```

## Custom Hooks

Prefix with `use`. Place in `src/hooks/`. Return a typed object, not a tuple (unless it's a simple pair like `[value, setter]`).

```typescript
// Good — hooks/useDriverStandings.ts
export function useDriverStandings(season: number) {
  const [standings, setStandings] = useState<DriverStanding[]>([]);
  ...
  return { standings, isLoading, error };
}
```

## API Calls

Use Axios. Centralize the client instance in `src/core/` or `src/api/`. Never call `axios.get(...)` directly in components — always go through the API layer.

```typescript
// Good — api/driversApi.ts
export async function fetchDriverStandings(season: number): Promise<DriverStanding[]> {
  const { data } = await apiClient.get<DriverStanding[]>(`/standings/${season}`);
  return data;
}
```

## File Size

Target 100–200 lines per file, max 300. Extract sub-components and hooks when a component grows.

## Package Manager

Use `bun` exclusively. Never use `npm` or `yarn`.

- Add packages: `bun add <package>`
- Dev packages: `bun add -d <package>`
- Run scripts: `bun run <script>`
- Execute binaries: `bunx <binary>`

## Lint & Format

Before finishing work on the frontend, mentally verify:
- `bun run lint` passes (ESLint)
- `bun run format:check` passes (Prettier, line width 120, single quotes)

Hooks auto-format `.ts`/`.tsx` files on save via Claude hooks. Still verify lint separately.
