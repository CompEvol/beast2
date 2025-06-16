
library(tidyverse)
setwd("~/WorkSpace/beast2/examples/testDirichlet/")

# Read tab-delimited file with header and skip comment lines
sim_a1111 <- read_tsv("simDirichletA1111.log", comment = "#")
sim_a2222 <- read_tsv("simDirichletA2222.log", comment = "#")
mcmc_no_prior <- read_tsv("dirichletNoPrior.log", comment = "#")
mcmc_a1111 <- read_tsv("dirichletAlpha1111.log", comment = "#")
mcmc_a2222 <- read_tsv("dirichletAlpha2222.log", comment = "#")
mcmc_a2AVMN <- read_tsv("dirichletAlpha2222AVMN.log", comment = "#")
mcmc_a1Bactrian <- read_tsv("BactrianAlpha1111.log", comment = "#")
mcmc_a2Bactrian <- read_tsv("BactrianAlpha2222.log", comment = "#")

cat("mcmc_data has ", nrow(sim_a1111), " samples, and sim_data has ", 
    nrow(mcmc_a1111), " samples.\n")
stopifnot(nrow(sim_a1111) == nrow(mcmc_a1111)-1)

# Combine and label the datasets
df_sim1 <- as_tibble(sim_a1111) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "SimAlpha1")
df_sim2 <- as_tibble(sim_a2222) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "SimAlpha2")
df_no_prior <- as_tibble(mcmc_no_prior) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "NoPrior")
df_mcmc1 <- as_tibble(mcmc_a1111) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "MCMCAlpha1")
df_mcmc2 <- as_tibble(mcmc_a2222) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "MCMCAlpha2")
df_a2AVMN <- as_tibble(mcmc_a2AVMN) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "AVMNAlpha2")
df_a1Bactrian <- as_tibble(mcmc_a1Bactrian) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "BactrianAlpha1")
df_a2Bactrian <- as_tibble(mcmc_a2Bactrian) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "BactrianAlpha2")

colMeans(df_sim1[,1:4])
sapply(df_sim1[, 1:4], var)

colMeans(df_sim2[,1:4])
sapply(df_sim2[, 1:4], var)

colMeans(df_no_prior[,1:4])
sapply(df_no_prior[, 1:4], var)

colMeans(df_mcmc1[,1:4])
sapply(df_mcmc1[, 1:4], var)

colMeans(df_mcmc2[,1:4])
sapply(df_mcmc2[, 1:4], var)


df_all <- bind_rows(df_sim1, df_sim2, df_no_prior, df_mcmc1, df_mcmc2, 
                    df_a2AVMN, df_a1Bactrian, df_a2Bactrian)
df_all_long <- df_all |> 
  pivot_longer(cols = 1:4, names_to = "dimension", values_to = "value")


p <- ggplot(df_all_long, aes(x = value, fill = source)) +
  facet_wrap(~dimension, scales = "free") +
  labs(title = paste0("Test Dirichlet")) +
  theme_minimal()
p1 <- p + geom_density(alpha = 0.5) + ylab("Density")
p1
ggsave(filename = "testDirichletMarginalDensity.pdf", plot = p1, 
       width = 8, height = 6)

p2 <- ggplot(df_all_long, aes(x = value, fill = source)) +
  facet_grid( source ~ dimension) +
  geom_histogram(bins = 100, alpha = 0.5, position = "identity") + 
  labs(title = paste0("Test Dirichlet")) + ylab("Frequency") +
  theme_minimal() + theme(legend.position = "none")
p2
ggsave(filename = "testDirichletMarginalDensity2.pdf", plot = p2, 
       width = 12, height = 12)


# https://www.blopig.com/blog/2019/06/a-brief-introduction-to-ggpairs/
library(GGally)

df_sub <- df_all |> 
  group_by(source) |> 
  slice_sample(prop = 0.01) |>  # 1% of each group
  ungroup()

p2 <- ggpairs(df_sub, mapping = ggplot2::aes(color = source, alpha = 0.3),
              lower = list( # shape = 1 for hollow circles
                continuous = wrap("smooth", shape = 1, size = .1, alpha = 0.1)  
              )) +
  labs(title = paste0("Test Dirichlet : 1% samlpes")) + 
  theme_minimal() 
p2
ggsave(filename = "subsampl-1per-Dirichlet.pdf", plot = p2, 
       width = 16, height = 16)

