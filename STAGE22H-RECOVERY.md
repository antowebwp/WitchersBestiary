# Stage 22H recovery

The checkpoint adds bounded sign visual packets and client geometry for Igni, Aard, Yrden and Axii. Quen still uses the pre-existing shell renderer. Geometry has compiled, but it has not been observed in a game client. No screenshot sheet exists.

Next: run a real client and capture all five signs and held G. Verify the debug-quad blend/depth behavior, terrain projection for Yrden, breath origin interpolation and packet expiry, Axii cancellation when control ends early, and Quen impact/variant geometry. Add sign textures and validate the 15 variants against actual combat forms. Run targeted sign regressions. Prior SignFormsTests logs show the embedded player losing its connection to Cardinal Components and casts subsequently being rejected; replace that fixture or exercise a connected modded client before attributing these failures to the input hotfix.
