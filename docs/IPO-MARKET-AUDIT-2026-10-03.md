# Axewatch Android audit - 3 October 2026

## Confirmed problems and changes

| Area | Problem | Change |
|---|---|---|
| Allotment | Rapid/repeated taps could launch concurrent checks; bulk button remained enabled | Set busy before coroutine launch, disable both actions, display family progress |
| Multiple PANs | An unexpected failure could stop later applicants | Isolate applicant failures; retain completed records and keep cancellation distinct |
| IPO identity | Generated short symbols can collide and select another company or duplicate picker keys | Carry company identity through selection, single/bulk checks and retries; reject ambiguous symbol-only lookups |
| Applicant selection | Secondary actions could silently use the first vault PAN | Require explicit selected/entered PAN; ambiguous saved-mask refresh still asks for entry |
| Allotment UX | Results were below long directories and management sections | Results directly under form; expandable vault, alerts and registrar tools; 48dp actions |
| Registrar links | Cameo host no longer resolves; Beetal old domain redirects | Current official Cameo status host and Beetal investor-services route |
| Source health | Any historical success kept the source operational after a later failure | Report the latest lookup outcome |
| Mutual funds | Same fabricated fees, AUM, returns and allocation on every fund | Nullable unsupported metrics; returns derived from dated NAV history |
| Fund loading | Eight sequential requests delayed all results; category chips did not match provider strings; one scheme code was empty | At most three concurrent requests, publish each success, normalized categories, current hybrid scheme code |
| Fund holdings | Portfolio did not include NAV prices; legacy mf types mismatched allocation logic | Combine stock prices and NAVs, load held scheme codes, normalize types without changing stored user quantities/costs |
| Share loading | A once-loaded constituent quote was never refreshed on reopening | Refresh the selected stock with a 60-second quote cache; preserve a known price if refresh fails |
| Share/index changes | A null holiday/future bar made the previous-close loop select the current session | Previous actual session selected by timestamp and exchange offset |
| Index loading | Membership stayed cached indefinitely for the process; failed loading required reopening | Daily view-model cache expiry and in-dialog retry |
| GMP | Positive fallback replaced valid zero/negative values; current IPOWatch has a mixed segment table and inline status | Explicit reported flag, per-row segment/status parsing, live-format fixtures |

## Live public checks

MUFG public page and GetDetails directory, KFintech landing page, Bigshare, Skyline,
Maashitla landing page/company directory, Purva, Cameo replacement and Beetal current
page responded successfully. NSE's official bid/allotment form was confirmed through
its public page; direct requests from this computer timed out. Public portal loading
is not a PAN-specific allotment verification or a CAPTCHA submission. No private PAN
was sent during this audit.

MFAPI's eight selected schemes were checked: the obsolete 120286 feed was empty;
120251 returns the current ICICI aggressive hybrid direct-growth scheme. Official
NIFTY membership CSV and Yahoo NSE quote responses were inspected. IPOWatch and
InvestorGain GMP/subscription HTML were inspected; minimal dated fixtures pin the
observed GMP structures.

## Verification and boundaries

The Android suite covers live-shaped parser fixtures, registrar status/quantity
handling, applicant failure/cancellation, migration and navigation, and narrow
allotment screenshots. Record fresh screenshots with the documented Roborazzi flag.
No Room schema, application ID, notification schedule or NSE bulk request budget
changes are needed. ADB reported no attached phone/emulator, so installation-over-
existing-app and real device network checks remain unverified.

The parent website frontend build passed. Docker rebuild/browser verification is
unavailable on this host because Docker Desktop's Linux engine is stopped and the
specified frontend/verify.mts file is absent. This change set is in the Android
repository and does not modify the website.

Local verification: `testDebugUnitTest assembleDebug -Proborazzi.test.record=true` passed. All 161 tests passed with zero failures, errors or skips. Fresh allotment and fund-detail screenshots were reviewed at 320dp with enlarged text. UTF-8/BOM and `git diff --check` audits passed. The release workflow repeats the full test suite and verifies the stable signing certificate and increasing version code before publishing an APK.
