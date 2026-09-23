# Implementation plan (superseded)

These five documents are the original bootstrap planning material. They have been **superseded** by the generated documentation set and are kept only so the reasoning that produced it is traceable.

| Original document | Superseded by |
| --- | --- |
| `00-roadmap.md` | [`docs/delivery/00-implementation-order.md`](../delivery/00-implementation-order.md) and [`docs/delivery/04-milestones.md`](../delivery/04-milestones.md) |
| `01-architecture-decisions-to-generate.md` | [`docs/architecture/adr/`](../architecture/adr/) - all 16 decisions now exist as ADRs |
| `02-document-generation-checklist.md` | [`docs/README.md`](../README.md) - every checklist item is now a document |
| `03-definition-of-done.md` | [`docs/delivery/02-definition-of-done.md`](../delivery/02-definition-of-done.md) - expanded from 12 lines to 12 sections |
| `04-coding-guardrails.md` | [`docs/architecture/03-compose-conventions.md`](../architecture/03-compose-conventions.md), [`docs/architecture/01-package-structure.md`](../architecture/01-package-structure.md), and the ADRs |
| `05-suggested-package-layout.md` | [`docs/architecture/01-package-structure.md`](../architecture/01-package-structure.md) - refined, with boundary enforcement and split triggers |

Two things in the originals were **changed** rather than merely expanded, and the change is recorded in [`docs/00-source-of-truth.md`](../00-source-of-truth.md):

- the roadmap's 26-issue backlog became 40 issues (R-16, `docs/delivery/05-issue-renumbering.md`);
- the suggested package root `com.example.productivitytrainer` became `com.wivernz.itera`, which the existing scaffold already uses.

Do not implement from these files. Start at [`docs/README.md`](../README.md).
