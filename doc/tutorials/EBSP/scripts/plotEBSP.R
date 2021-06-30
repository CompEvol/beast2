getPopSize <- function(t, changeTimes, changePops, isLinear) {

    i <- findInterval(t, changeTimes)

    if (isLinear) {
        if (i>=length(changeTimes))
            return (changePops[length(changePops)])

        if (i<1)
            return (changePops[1])

        return (changePops[i] + (t-changeTimes[i])/(changeTimes[i+1]-changeTimes[i])*(changePops[i+1]-changePops[i]))
    } else {
        return (changePops[min(length(changePops), max(0, i))])
    }
}

# Adds a population function to an existing plot
plotPopFunction <- function(changeTimes, changePops, isLinear, maxTime=NA, ...) {
    if (isLinear) {
        lines(changeTimes, changePops, ...)
    } else {
        lines(changeTimes, changePops, 's', ...)
    }

    if (!is.na(maxTime) && maxTime>changeTimes[length(changeTimes)])
        lines(c(changeTimes[length(changeTimes)], maxTime), rep(changePops[length(changePops)],2), ...)
}

# This is an approximation that assumes HPD is a single interval
computeHPD <- function(x, alpha=0.95, sorted=FALSE) {
    if (sorted)
        y <- x
    else
        y <- sort(x)

    n <- length(y)
    m <- round(alpha*n)

    i <- 1
    delta <- y[n] - y[1]
    lower <- y[1]
    upper <- y[n]

    while (i+m <= n) {
        thisDelta <- y[i+m] - y[i]
        if (thisDelta < delta) {
            delta <- thisDelta
            lower <- y[i]
            upper <- y[i+m]
        }

        i <- i + 1
    }

    res <- list()
    res$lower <- lower
    res$upper <- upper

    return(res)
}

# Compute all relevant statistics from EBSP data.
processEBSPdata <- function(df, isLinear) {

    frameLen <- dim(df)[1]
    nTimes <- dim(df)[2] - 2

    allTimes <- rep(0, nTimes)

    changeTimes <- list()
    changePops <- list()

    Nmedian <- rep(0, nTimes)
    NupperHPD <- rep(0, nTimes)
    NlowerHPD <- rep(0, nTimes)
    NupperCPD <- rep(0, nTimes)
    NlowerCPD <- rep(0, nTimes)

    for (i in 1:frameLen) {
        theseChangeTimes <- NULL
        theseChangePops <- NULL
        p <- 0
        for (j in 1:nTimes) {
            pair = strsplit(as.character(df[i,1+j]), ":")[[1]]
            time <- as.double(pair[1])
            allTimes[j] <- allTimes[j] + time

            if (length(pair)>1) {
                p <- p + 1
                theseChangeTimes[p] <- time
                theseChangePops[p] <- as.double(pair[2])
            }
        }

        changeTimes[[i]] <- theseChangeTimes
        changePops[[i]] <- theseChangePops
    }

    allTimes <- allTimes/frameLen

    for (i in 1:nTimes) {
        thisN <- rep(0, frameLen)
        for (j in 1:frameLen) {
            thisN[j] <- getPopSize(allTimes[i], changeTimes[[j]], changePops[[j]], isLinear)
        }
        Nmedian[i] <- median(thisN)

        # Compute confidence intervals

        hpd <- computeHPD(thisN, alpha=0.95)
        NlowerHPD[i] <- hpd$lower
        NupperHPD[i] <- hpd$upper

        NlowerCPD[i] <- quantile(thisN, probs=0.025)
        NupperCPD[i] <- quantile(thisN, probs=0.975)
    }

    res <- list()
    res$allTimes <- allTimes
    res$changeTimes <- changeTimes
    res$changePops <- changePops
    res$Nmedian <- Nmedian
    res$NlowerHPD <- NlowerHPD
    res$NupperHPD <- NupperHPD
    res$NlowerCPD <- NlowerCPD
    res$NupperCPD <- NupperCPD

    return(res)
}

getTimes <- function(df) {

    frameLen <- dim(df)[1]
    nTimes <- dim(df)[2] - 2

    times <- rep(0, frameLen*nTimes)
    k <- 1

    for (i in 1:frameLen) {
        for (j in 1:nTimes) {
            times[k] <- as.double(strsplit(df[i,1+j], ":")[[1]][1])
            k <- k + 1
        }

    }

    return(times)
}
removeBurnin <- function(df, burnin=0.1) {
    frameLen <- dim(df)[1]
    return(df[-(1:ceiling(burnin*frameLen)),])
}

plotEBSP <- function(fileName, burnin=0.1, isLinear=TRUE, useHPD=TRUE, showLegend=TRUE, plotPopFunctions=FALSE, popFunctionAlpha=0.05, ...) {

    df <- removeBurnin(read.table(fileName, header=T, sep='\t', as.is=T), burnin)
    res <- processEBSPdata(df, isLinear)

    ellipsis <- list(...)

    if (length(ellipsis$xlab) == 0)
        ellipsis$xlab = "Time"

    if (length(ellipsis$ylab) == 0)
        ellipsis$ylab = "Population"

    if (length(ellipsis$ylim) == 0) {
        if (useHPD)
            ellipsis$ylim = c(0.9*min(res$NlowerHPD), 1.1*max(res$NupperHPD))
        else
            ellipsis$ylim = c(0.9*min(res$NlowerCPD), 1.1*max(res$NupperCPD))
    }

    args <- c(list(res$allTimes, res$Nmedian, 'l'), ellipsis)

    do.call(plot, args)

    if (!plotPopFunctions) {
        if (useHPD)
            polygon(c(res$allTimes, rev(res$allTimes)), c(res$NlowerHPD, rev(res$NupperHPD)),
                    col="grey", border=NA)
        else
            polygon(c(res$allTimes, rev(res$allTimes)), c(res$NlowerCPD, rev(res$NupperCPD)),
                    col="grey", border=NA)
    } else {
        maxTime <- max(res$allTimes)
        for (i in 1:length(res$changeTimes)) {
            plotPopFunction(res$changeTimes[[i]], res$changePops[[i]], isLinear, maxTime=maxTime, col=rgb(0, 0.5, 0, popFunctionAlpha))
        }
    }

    if (useHPD) {
        lines(res$allTimes, res$NupperHPD, lwd=1)
        lines(res$allTimes, res$NlowerHPD, lwd=1)
    } else {
        lines(res$allTimes, res$NupperCPD, lwd=1)
        lines(res$allTimes, res$NlowerCPD, lwd=1)
    }

    lines(res$allTimes, res$Nmedian, lty=2, lwd=2)

    if (showLegend) {
        if (useHPD)
            CIlabel <- "95% HPD"
        else
            CIlabel <- "95% CPD"

        legend('topright', inset=0.05, c("Median", CIlabel), lty=c(2, 1), lwd=c(2, 1))
    }
    return(data.frame(Time=res$allTimes, Median=res$Nmedian, HPDlow=res$NlowerHPD, HPDupper=res$NupperHPD))
}

plotEBSPTimesHist <- function(fileName, burnin=0.1, alpha=0.95, ...) {

    df <- removeBurnin(read.table(fileName, header=T, sep='\t', colClasses="character"), burnin)
    times <- getTimes(df)

    xmax <- quantile(times, alpha)

    hist(times, breaks=c(seq(0, xmax, length.out=100), max(times)), xlim=c(0, xmax),
         xlab="Time", prob=T, main="Histogram of tree event times in log")

}

