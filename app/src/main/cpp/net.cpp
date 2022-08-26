#include "net.h"
#define TAG "net"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
Inference_engine::Inference_engine()
{ }

Inference_engine::~Inference_engine() {
    if ( netPtr != NULL ) {
		if ( sessionPtr != NULL) {
			netPtr->releaseSession(sessionPtr);
			sessionPtr = NULL;
		}

		delete netPtr;
		netPtr = NULL;
	}
}

int Inference_engine::load_param(std::string & file, int num_thread) {

    LOGD("file %s", file.c_str());
    if (!file.empty()) {

        if (file.find(".mnn") != std::string::npos) {
            LOGD("create netPtr");
	        netPtr = MNN::Interpreter::createFromFile(file.c_str());
            if (nullptr == netPtr) return -1;
            LOGD("create netPtr 111");
            MNN::ScheduleConfig sch_config;
            //sch_config.type = (MNNForwardType)MNN_FORWARD_OPENCL;
            sch_config.type = (MNNForwardType)MNN_FORWARD_CPU;
            if ( num_thread > 0 )sch_config.numThread = num_thread;
            LOGD("create netPtr 222");
            MNN::BackendConfig backendConfig;

            backendConfig.precision = (MNN::BackendConfig::PrecisionMode)2;
            sch_config.backendConfig = &backendConfig;
            LOGD("create netPtr 333");
            sessionPtr = netPtr->createSession(sch_config);
            LOGD("create netPtr 444");
            if (nullptr == sessionPtr) return -1;
        } else {
            return -1;
        }
    }

    return 0;
}

int Inference_engine::set_params(int srcType, int dstType, 
                                 float *mean, float *scale) {
    config.destFormat   = (MNN::CV::ImageFormat)dstType;
    config.sourceFormat = (MNN::CV::ImageFormat)srcType;

    ::memcpy(config.mean,   mean,   3 * sizeof(float));
    ::memcpy(config.normal, scale,  3 * sizeof(float));

    config.filterType = (MNN::CV::Filter)(1);
    config.wrap = (MNN::CV::Wrap)(2);

    return 0;
}

// infer
int Inference_engine::infer_img(unsigned char *data, int width, int height, int channel, int dstw, int dsth, Inference_engine_tensor& out) {
    MNN::Tensor* tensorPtr = netPtr->getSessionInput(sessionPtr, nullptr);
    MNN::CV::Matrix transform;
    LOGD("infer 111");
    int h = height;
    int w = width;
    int c = channel;
    // auto resize for full conv network.
    bool auto_resize = false;
    if ( !auto_resize ) {
        std::vector<int>dims = { 1, c, dsth, dstw };
        LOGD("infer 111--111");
        netPtr->resizeTensor(tensorPtr, dims);
        LOGD("infer 111--222");
        netPtr->resizeSession(sessionPtr);
        LOGD("infer 111--333");
    }
    LOGD("infer 222");

    transform.postScale(1.0f/dstw, 1.0f/dsth);
    transform.postScale(w, h);
    LOGD("infer 333");

    std::unique_ptr<MNN::CV::ImageProcess> process(MNN::CV::ImageProcess::create(config.sourceFormat, config.destFormat, config.mean, 3, config.normal, 3));

    process->setMatrix(transform);
    LOGD("infer 444");

    process->convert(data, w, h, w*c, tensorPtr);
    netPtr->runSession(sessionPtr);
    LOGD("infer 555");

    for (int i = 0; i < out.layer_name.size(); i++) {
        const char* layer_name = NULL;
        if( strcmp(out.layer_name[i].c_str(), "") != 0) {
            layer_name = out.layer_name[i].c_str();
        }
        MNN::Tensor* tensorOutPtr = netPtr->getSessionOutput(sessionPtr, layer_name);

        std::vector<int> shape = tensorOutPtr->shape();

        auto tensor = reinterpret_cast<MNN::Tensor*>(tensorOutPtr);

        std::unique_ptr<MNN::Tensor> hostTensor(new MNN::Tensor(tensor, tensor->getDimensionType(), true));
        tensor->copyToHostTensor(hostTensor.get());
        tensor = hostTensor.get();

        auto size = tensorOutPtr->elementSize();
        std::shared_ptr<float> destPtr(new float[size * sizeof(float)]);

        ::memcpy(destPtr.get(), tensorOutPtr->host<float>(), size * sizeof(float));

        out.out_feat.push_back(destPtr);
    }

    return 0;
}
